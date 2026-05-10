# 🛒 High-Level Design (HLD) — Cart System
> **Target Level:** Staff / Principal Engineer
> **Interview Focus:** Distributed Consistency, Concurrent Writes, Cache Strategy, Checkout Contract, Multi-Region

---

## 1. Requirements

### 1.1 Functional Requirements
- Users can **add**, **update quantity**, and **remove** items from their cart.
- Users can **view** the current state of their cart (with live prices + inventory status).
- Cart is **user-specific** (authenticated) and supports **guest carts** (session-based).
- Guest cart is **merged** into user cart upon login/registration.
- Cart enforces **inventory availability checks** (soft reservation at add; hard lock at checkout).
- Cart is **persisted** — survives page refresh, browser close, and device switch.
- Support **promo codes / coupons** applied to the cart (validated at add and re-validated at checkout).
- Cart reflects **real-time price changes** from the catalog.
- Support **multi-device sync** — same cart visible across web, iOS, Android.
- **Checkout handoff** — cart service hands off a validated, locked snapshot to Order Service.

### 1.2 Non-Functional Requirements

| Property | Target | Notes |
|---|---|---|
| **Read Latency** | < 20 ms P99 | Redis-served reads |
| **Write Latency** | < 80 ms P99 | Includes inventory soft-check |
| **Availability** | 99.99% | Fail-open on dependency failures |
| **Consistency** | Strong for qty/inventory; Eventual for price | Documented per-field |
| **Scale** | 100M DAU, 10M concurrent carts, 50K RPS peak | |
| **Durability** | Zero cart data loss | WAL + replication |
| **Multi-device** | Cart synced within 2 sec across devices | |
| **Cart TTL** | Active cart: 30 days; Guest cart: 7 days | Refreshed on access |

### 1.3 Out of Scope
- Payment processing
- Order placement / post-checkout flow
- Wishlist / Save-for-later
- DDoS protection (WAF/CDN layer)

---

## 2. Capacity Estimation

```
DAU                     = 100 million
Avg cart ops/user/day   = 10  (add, view, update, remove)
Avg RPS                 = 100M × 10 / 86,400 ≈ 11,600 RPS
Peak RPS (flash sale)   ≈ 5× avg = ~50,000 RPS

Cart payload (avg)      = 5 items × 200B = ~1 KB per cart
Redis memory            = 10M concurrent carts × 1 KB = 10 GB (fits in Redis)
  + 20% overhead        → ~12 GB → size cluster at 24 GB for 2× headroom

DynamoDB storage        = 100M carts × 1 KB = 100 GB
  (30-day active carts; archived carts to S3 / cold tier)

Kafka throughput        = 50K cart events/sec × 500 B = ~25 MB/s (comfortable)
Bandwidth (read)        = 50K RPS × 1 KB = ~50 MB/s egress
Inventory Svc calls     = 50K RPS × 1 (soft check on add) = 50K calls/sec
  → mitigated: batch-check per cart on view; local cache on Inventory status
```

---

## 3. High-Level Architecture

```
  Mobile / Web / iOS
         │
         ▼
  ┌──────────────────┐
  │    CDN / Edge    │  (Cache static product images; not cart API)
  └──────┬───────────┘
         │
  ┌──────▼───────────┐
  │   API Gateway    │  Auth (JWT/OAuth2), Rate Limiting, Routing, SSL termination
  └──────┬───────────┘
         │
  ┌──────▼───────────────────────────────────────┐
  │              Cart Service                     │
  │  (Stateless, horizontally scaled)             │
  │                                               │
  │  Write Path:  validate → check inventory      │
  │               → atomic Redis write (Lua)      │
  │               → async DB persist              │
  │               → publish Kafka event           │
  │                                               │
  │  Read Path:   Redis HIT → enrich prices       │
  │               Redis MISS → DB → warm cache    │
  └──┬──────────────┬──────────────────┬──────────┘
     │              │                  │
┌────▼────┐  ┌──────▼──────┐  ┌───────▼──────┐
│  Redis  │  │  DynamoDB   │  │  Kafka Bus   │
│ Cluster │  │ (Source of  │  │  (Cart       │
│ (Hot    │  │  Truth)     │  │   Events)    │
│ Cache)  │  └─────────────┘  └──────┬───────┘
└─────────┘                          │
                          ┌──────────┼───────────────┐
                     ┌────▼────┐ ┌───▼──────┐ ┌──────▼────┐
                     │Analytics│ │Notif Svc │ │ Search /  │
                     │         │ │(abandon) │ │ Recommend │
                     └─────────┘ └──────────┘ └───────────┘

  Downstream sync (async):
  ┌─────────────┐   ┌────────────────┐   ┌──────────────┐
  │  Inventory  │   │  Catalog Svc   │   │  Promo/Coupon│
  │  Service    │   │  (Prices)      │   │  Service     │
  └─────────────┘   └────────────────┘   └──────────────┘
```

---

## 4. API Design

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/v1/cart/{userId}` | Fetch cart with enriched prices + inventory status |
| `POST` | `/v1/cart/{userId}/items` | Add item (idempotency key required) |
| `PATCH` | `/v1/cart/{userId}/items/{itemId}` | Update qty (idempotency key + version) |
| `DELETE` | `/v1/cart/{userId}/items/{itemId}` | Remove item |
| `POST` | `/v1/cart/merge` | Merge guest → user cart on login |
| `POST` | `/v1/cart/{userId}/coupon` | Apply/remove coupon |
| `POST` | `/v1/cart/{userId}/checkout-snapshot` | Produce validated, locked cart snapshot for Order Svc |

**gRPC** used for inter-service calls (Cart → Inventory, Cart → Catalog) for lower latency.

**Idempotency:** All write APIs require `Idempotency-Key: <uuid>` header. Key stored in Redis with 24h TTL; duplicate requests return cached response.

---

## 5. Data Model

### 5.1 Redis (Hot Cache) — HASH per cart

```
Key:    cart:{userId}
TTL:    30 days (refreshed on any access)
Fields:
  version          → INT  (monotonic version for optimistic locking)
  items            → JSON array of CartItem
  coupon_code      → STRING (nullable)
  coupon_discount  → DECIMAL
  updated_at       → UNIX timestamp

CartItem:
  { itemId, productId, sku, qty, addedAt, inventoryStatus }

Note: price NOT stored → fetched live from Catalog at read time
```

### 5.2 DynamoDB (Source of Truth)

```
Table: Carts
  PK: userId            (partition key)
  SK: "CART"            (static; allows future per-item rows in same table)
  version: INT          (optimistic lock version, matches Redis)
  items: LIST<CartItem>
  coupon_code: STRING
  updated_at: TIMESTAMP
  ttl: EPOCH            (DynamoDB TTL for auto-expiry after 30 days)

Table: GuestCarts
  PK: sessionId
  SK: "CART"
  ttl: EPOCH (7 days)

GSI: guestSessionId-index → for merge lookup

Consistency model:
  Cart reads during browsing   → Eventually consistent (faster, cheaper)
  Cart reads at checkout       → Strongly consistent (correct)
```

### 5.3 DynamoDB Hot Partition Analysis (Staff-Level)

```
Problem: userId as PK works well for uniform distribution.
         BUT during a flash sale, 1M users add the SAME product simultaneously.
         The bottleneck is NOT DynamoDB (each user has their own partition).
         The bottleneck is Inventory Service — all 1M calls hit item's inventory record.

Mitigation for Inventory hotspot:
  - Inventory counter sharded: item_stock:{productId}:shard:{0..N}
  - Cart Service reads sum(shards); decrements random shard
  - Write amplification acceptable for high-velocity items

DynamoDB WCU sizing:
  50K RPS writes × 1 KB payload = 50K WCU/sec
  DynamoDB on-demand handles this without pre-provisioning
  At provisioned mode: 50K WCU × $0.00065/WCU-hr = ~$32.50/hr → use on-demand for flash sales
```

---

## 6. Concurrent Write Conflicts — Version Vectors (Staff-Level)

```
Problem: User opens cart on mobile + web simultaneously.
         Both read version=5, both update qty, both try to write version=6.
         Last write wins → one update silently lost.

Solution: Optimistic locking with monotonic version

Write flow (Lua script — atomic):
  local current_version = redis.call('HGET', key, 'version')
  if current_version ~= ARGV['expected_version'] then
      return 'VERSION_CONFLICT'   -- client retries with fresh read
  end
  redis.call('HSET', key, 'version', current_version + 1)
  redis.call('HSET', key, 'items', ARGV['items'])
  return 'OK'

Client behavior on VERSION_CONFLICT:
  1. Re-fetch cart (get latest version)
  2. Re-apply user's intended delta (not the full payload)
  3. Retry write

API contract:
  PATCH /v1/cart/{userId}/items/{itemId}
  Body: { qty: 3, expected_version: 5 }
  Response 409 Conflict → { current_version: 6, current_cart: {...} }

DynamoDB conditional write mirrors this:
  ConditionExpression: version = :expected_version
  UpdateExpression: SET version = version + 1, items = :items
  → ConditionalCheckFailedException → 409 to client
```

---

## 7. Cache Strategy — Deep Dive

### 7.1 Write Path: Write-Through with Async DB Persist

```
Client Write
     │
     ▼
Cart Service
     │
     ├─── 1. Lua atomic write to Redis (version check + HSET)  ← synchronous
     │
     ├─── 2. Publish CartItemAdded to Kafka                     ← synchronous (fire)
     │
     └─── 3. Async DB persist (Kafka consumer → DynamoDB)       ← async

Why async DB write?
  Redis write = <1ms; DynamoDB write = 5-10ms
  Making DynamoDB synchronous adds 5-10ms to every write → violates P99 target
  Trade-off: tiny window where Redis has data but DynamoDB doesn't
  Recovery: on Redis miss, DynamoDB is always source of truth (Kafka guarantees delivery)

Failure case: Redis write succeeds, Kafka publish fails
  → Retry Kafka publish (in-process retry, 3 attempts, 50ms backoff)
  → If all retries fail: write directly to DynamoDB synchronously (fallback)
  → Alert + dead-letter queue for investigation
```

### 7.2 Read Path: Cache-Aside with Stampede Prevention

```
GET /v1/cart/{userId}
     │
     ├─ Redis HIT (>90% cases) → enrich prices from Catalog cache → return
     │
     └─ Redis MISS
           │
           ├─ Acquire distributed lock: SET lock:{userId} NX EX 2
           │    (only 1 of N concurrent requests proceeds to DB)
           │
           ├─ Re-check Redis (another thread may have populated it)
           │
           ├─ DynamoDB read (strongly consistent)
           │
           ├─ Write to Redis + set TTL
           │
           └─ Release lock; other waiters served from Redis

Cache Stampede (e.g., 10K users open same abandoned cart link simultaneously):
  Without lock: 10K DynamoDB reads in parallel → DB overload
  With lock: 1 DB read, 9,999 Redis reads → safe

Probabilistic early expiry (alternative):
  When TTL < threshold, random % of reads trigger background refresh
  → Prevents expiry spike without needing explicit locks
  → Simpler but slightly stale data possible (acceptable for cart views)
```

### 7.3 Price Enrichment at Read Time

```
Cart GET response:
  1. Load cart items (productIds) from Redis
  2. Batch fetch prices from Catalog Service:
       gRPC: GetPrices(productIds: [...])
       → Catalog has its own Redis cache (TTL: 5 min)
       → Cart Service also keeps local Caffeine cache (TTL: 30 sec, max 10K entries)
  3. Merge: cart items + live prices → enriched response
  4. If Catalog unreachable: serve last cached price + X-Price-Freshness: stale header

Why price not stored in cart?
  → Price changes (promotions, dynamic pricing) must reflect immediately
  → Storing price creates consistency nightmare (stale price = wrong total shown)
  → Re-fetched at checkout anyway — no benefit to storing in cart

Latency budget for enrichment:
  Redis read:          ~1 ms
  Catalog gRPC call:   ~5 ms (local cache hit) / ~15 ms (Catalog Redis hit)
  Total read P99:      ~20 ms  ✅
```

---

## 8. Inventory Integration — Soft vs Hard Reservation

| Operation | Strategy | Latency Impact | Risk |
|---|---|---|---|
| View Cart | Async event-driven (Kafka) | 0 ms | Shows stale OOS status briefly |
| Add to Cart | Sync soft-check (gRPC) | +10 ms | May allow adds for borderline stock |
| Checkout Snapshot | Hard lock (distributed txn) | +50 ms | Correctness critical |

```
Soft Reservation (Add to Cart):
  Cart Svc → gRPC → Inventory Svc: CheckAvailability(productId, qty)
  Inventory returns: AVAILABLE | LOW_STOCK | OUT_OF_STOCK
  Cart Svc:
    AVAILABLE   → add to cart, set inventoryStatus=AVAILABLE
    LOW_STOCK   → add to cart, set inventoryStatus=LOW_STOCK, warn user
    OUT_OF_STOCK → reject add, return 409

Event-Driven OOS Update (Kafka consumer in Cart Svc):
  Topic: inventory.stock_updated
  Consumer: Cart Service
  On event: if stock → 0 for productId
    → Scan Redis for carts containing productId (via secondary index or Kafka-fan-out)
    → Update inventoryStatus=OUT_OF_STOCK in those cart items
    → Push WebSocket notification to active users

Secondary index for "carts containing productId":
  Key: product_cart_index:{productId}
  Type: Redis SET of userIds
  → On add-to-cart: SADD product_cart_index:{productId} {userId}
  → On remove: SREM product_cart_index:{productId} {userId}
  → On OOS event: SMEMBERS product_cart_index:{productId} → update those carts
  Trade-off: ~100B extra Redis memory per (product, user) pair; acceptable
```

---

## 9. Checkout Handoff Contract (Staff-Level)

```
This is the boundary between Cart Service and Order Service.
Cart Service must provide a VALIDATED, POINT-IN-TIME SNAPSHOT.

POST /v1/cart/{userId}/checkout-snapshot
  1. Read cart from DynamoDB (strongly consistent read)
  2. Hard inventory reservation:
       gRPC: ReserveInventory(items: [...], reservationTTL: 10 min)
       → Inventory Svc atomically decrements stock, returns reservationId
       → If any item OOS → return 409 with which items failed
  3. Validate coupon (re-call Coupon Svc — coupons may have expired)
  4. Fetch final prices from Catalog (strongly consistent)
  5. Build CartSnapshot:
       {
         snapshotId: uuid,
         userId,
         items: [{ productId, qty, unitPrice, subtotal }],
         couponCode, couponDiscount,
         totalAmount,
         reservationId,       ← ties snapshot to inventory hold
         reservationExpiresAt,
         createdAt
       }
  6. Store snapshot in DynamoDB (CartSnapshots table, TTL: 15 min)
  7. Return snapshotId to client
  8. Client passes snapshotId to Order Service → Order Svc validates snapshot integrity

Why a snapshot?
  → Decouples cart (mutable) from order (immutable contract)
  → Price/stock locked at point of checkout intent
  → Prevents race: user modifies cart while payment is processing
  → Order Svc doesn't need to re-validate anything — trusts the snapshot
```

---

## 10. Guest Cart & Merge Flow

```
Guest User adds items → cart:guest:{sessionId} (Redis, 7-day TTL)
                                    │
                           User logs in / signs up
                                    │
                     POST /v1/cart/merge { guestSessionId, userId }
                                    │
              ┌─────────────────────▼──────────────────────┐
              │           Merge Strategy                    │
              │                                             │
              │  For each item in guest cart:               │
              │    if item exists in user cart:             │
              │      qty = MAX(guest_qty, user_qty)         │
              │      (don't double-count; take intent)      │
              │    else:                                     │
              │      add item to user cart                  │
              │                                             │
              │  Re-validate all merged items:              │
              │    → Soft inventory check on merged list     │
              │    → Mark OOS items with warning             │
              │                                             │
              │  Atomicity: Lua script merges both          │
              │  carts in one Redis transaction             │
              └─────────────────────────────────────────────┘
                                    │
                    Delete guest cart (Redis DEL + DynamoDB delete)
                                    │
                    Emit CartMerged event → Kafka
                    (Analytics: track guest→user conversion)

Edge cases:
  Guest cart expired (>7 days): merge skipped; user cart untouched
  User cart doesn't exist yet: guest cart promoted to user cart (rename key)
  Merge conflict (item removed from inventory between sessions): mark as OOS
```

---

## 11. Multi-Region Active-Active Design (Staff-Level)

```
Target: Global platform with users in US, EU, APAC.
Goal: Cart reads/writes served from nearest region (<50ms round trip).

Architecture: Active-Active with Conflict Resolution

  US-EAST region          EU-WEST region          APAC region
  ┌──────────────┐        ┌──────────────┐        ┌──────────────┐
  │ Cart Service │        │ Cart Service │        │ Cart Service │
  │ + Redis      │        │ + Redis      │        │ + Redis      │
  │ + DynamoDB   │◀──────▶│ + DynamoDB   │◀──────▶│ + DynamoDB   │
  │  Global Table│        │  Global Table│        │  Global Table│
  └──────────────┘        └──────────────┘        └──────────────┘
         ▲                        ▲                       ▲
         │        Global Traffic Manager (Route 53 Latency)│
         └────────────────────────┴───────────────────────┘

DynamoDB Global Tables:
  - Multi-master replication across regions
  - Last-writer-wins conflict resolution (timestamp-based)
  - Replication lag: <1 sec typical

Conflict scenario (user on VPN, switches regions mid-session):
  - User in US adds item A (version=5 → 6)
  - Before replication, user connects to EU, adds item B (also sees version=5 → 6)
  - DynamoDB last-writer-wins: one update survives
  - Mitigation: version vector + merge strategy (same as concurrent write handling)
  - For most users (single region, single device): no conflict

Redis per region:
  - Write to local Redis only
  - Cross-region Redis sync NOT done (too complex, high latency)
  - Source of truth is always DynamoDB Global Table
  - On cross-region session: Redis MISS → DynamoDB read → warm local Redis

User affinity:
  Route 53 sticky routing → user always hits same region unless that region is down
  Failover: Route 53 health checks → reroute within 30 sec
```

---

## 12. Cart Cleanup & Expiry Strategy

```
Problem: 100M users, 30-day TTL. How do abandoned carts get cleaned up?

Layer 1 — Redis TTL:
  cart:{userId} TTL = 30 days, refreshed on every access
  Redis auto-evicts expired keys → no cleanup needed at Redis layer

Layer 2 — DynamoDB TTL:
  ttl attribute set to (now + 30 days) on every write
  DynamoDB TTL daemon deletes expired items within 48 hours
  → No application-level cleanup required

Layer 3 — Cart Abandonment Detection (Business):
  Scheduled job (every hour): scan Kafka CartViewed events
  If cart not viewed in 24 hours → publish CartAbandoned event
  Notification Service consumes → sends retargeting email/push
  Analytics: track abandonment rate per product category

Layer 4 — Archival for Analytics:
  DynamoDB Streams → Lambda → S3 (Parquet) on cart deletion
  Athena queries for cart abandonment analysis
  Retention: 1 year in S3 cold tier
```

---

## 13. CQRS — Separate Read Model for Cart Analytics

```
Cart Service owns the write model (Redis + DynamoDB).
Analytics needs a different shape: "which products are most carted but not purchased?"

CQRS pattern:
  Write side → Cart Service → Kafka
  Read side  → Analytics Consumer → ClickHouse / Redshift

Kafka topics:
  cart.item_added    → { userId, productId, qty, timestamp, deviceType }
  cart.item_removed  → { userId, productId, reason: [user_action, oos, price_change] }
  cart.viewed        → { userId, cartValue, itemCount, timestamp }
  cart.abandoned     → { userId, cartValue, items, lastViewedAt }
  cart.checkout      → { userId, cartValue, conversionTime }

ClickHouse read model (updated in near real-time):
  Table: cart_product_funnel
    productId, carted_count, purchased_count, abandoned_count, avg_cart_duration

  Queries:
    → "Top 100 products added to cart but never purchased" (merchandising signal)
    → "Average cart abandonment time by product category"
    → "Conversion rate by coupon code"

Cart Service does NOT query ClickHouse → strict CQRS separation.
```

---

## 14. Failure Handling Matrix

| Failure | Detection | Strategy | User Impact |
|---|---|---|---|
| **Redis node down** | Health check <5s | Failover to replica; reads degrade to DynamoDB | Latency +10ms |
| **Redis cluster split** | Cluster bus timeout | Reject writes; serve reads from DynamoDB | Write 503 briefly |
| **DynamoDB unavailable** | gRPC timeout | Serve reads from Redis; buffer writes in Kafka | Writes deferred |
| **Inventory Svc down** | Circuit breaker (5 failures/10s) | Allow add-to-cart with `inventoryStatus=UNKNOWN` + warning banner | Soft degradation |
| **Catalog Svc down** | Circuit breaker | Serve last cached price + staleness header | Stale price shown |
| **Coupon Svc down** | Timeout (200ms) | Skip coupon validation; apply at checkout | Coupon not validated |
| **Kafka down** | Producer error | Synchronous DynamoDB write fallback; retry Kafka | No analytics gap |
| **Cart Svc instance crash** | LB health check | Route to healthy instances (stateless) | Transparent |

```
Circuit Breaker config (Resilience4j):
  failureRateThreshold:     50%
  waitDurationInOpenState:  10 sec
  slidingWindowSize:        20 calls
  permittedCallsInHalfOpen: 3

Timeout hierarchy:
  Redis:        5 ms timeout (fail fast)
  Inventory:   50 ms timeout (soft check)
  Catalog:     30 ms timeout (price enrichment)
  DynamoDB:   100 ms timeout (source of truth read)
```

---

## 15. Security

| Concern | Implementation |
|---|---|
| **Authentication** | JWT (RS256); guest = signed session token (HMAC-SHA256, 7-day TTL) |
| **Authorization** | Cart Svc validates `sub` (userId) in JWT = path param userId; 403 on mismatch (IDOR prevention) |
| **Rate Limiting** | 100 cart writes/min per user; 500 reads/min; enforced at API Gateway (Redis-backed) |
| **Input Validation** | Max qty per item: 99; max items per cart: 50; productId allowlist via Catalog Svc |
| **Guest cart isolation** | sessionId = cryptographically random UUID; not guessable |
| **PII** | No sensitive PII in cart; userId is internal opaque ID |
| **Coupon abuse** | One-time coupon: atomic Redis SETNX on `coupon_used:{couponId}:{userId}` |

---

## 16. Observability

| Signal | Metric | Alert Threshold |
|---|---|---|
| **Read latency** | `cart.get.latency_p99` | > 50 ms |
| **Write latency** | `cart.write.latency_p99` | > 150 ms |
| **Cache hit rate** | `redis.cart.hit_rate` | < 85% |
| **Version conflicts** | `cart.write.version_conflict_rate` | > 1% |
| **Inventory check failures** | `cart.inventory.circuit_open` | Any |
| **Cart abandonment rate** | `cart.abandoned / cart.viewed` | > baseline + 20% |
| **Checkout snapshot failures** | `cart.checkout_snapshot.error_rate` | > 0.1% |
| **Kafka consumer lag** | `cart.kafka.consumer_lag` | > 10K messages |
| **DynamoDB throttle** | `ddb.throttled_requests` | > 0 |

**Tracing:** OpenTelemetry spans: `cart.get → redis.get → catalog.getprices → response`
**Logs:** Structured JSON; `cartId`, `userId`, `traceId`, `version` on every log line.

---

## 17. Key Design Decisions & Trade-offs

| Decision | Choice | Rejected Alternative | Why |
|---|---|---|---|
| **Price storage** | Not stored; fetched live | Store in cart | Live fetch ensures freshness; re-validated at checkout anyway |
| **Write strategy** | Write-through Redis + async DynamoDB | Synchronous dual write | Async saves 10ms per write; Kafka guarantees durability |
| **Inventory check** | Soft at add; hard at checkout | Hard lock at add | Hard lock at add hurts UX; oversell risk handled at checkout |
| **Concurrent writes** | Optimistic locking (version) | Pessimistic lock | Pessimistic locks cause head-of-line blocking at scale |
| **Cache stampede** | Distributed lock on miss | No protection | Without lock, 1K concurrent misses = 1K DynamoDB reads |
| **DB choice** | DynamoDB | Cassandra | Managed, auto-scaling, global tables built-in; no ops burden |
| **Guest cart** | Redis only, 7-day TTL | DynamoDB | Guest carts are ephemeral; Redis is sufficient and cheap |
| **Checkout** | Snapshot model | Live cart at checkout | Snapshot prevents mid-payment cart modification race |
| **Multi-region** | DynamoDB Global Tables | Custom sync | Managed replication; last-writer-wins acceptable for carts |
| **Read model** | CQRS via Kafka | Direct DB queries | Decouples analytics from operational load; enables ClickHouse |

---

## 18. Future Enhancements

- **Shared / Collaborative Carts** — family or team members share one cart; requires user-level ACLs and merge conflict UI.
- **Save for Later / Wishlist** — items moved out of active cart; separate `SavedItems` table, same service.
- **Dynamic / Personalized Pricing** — price shown in cart = function of user segment; Catalog Svc returns per-user price on request.
- **ML Cart Recommendations** — "Frequently bought together" panel driven by co-occurrence model on cart events in ClickHouse.
- **Cost-aware Rate Limiting** — flash-sale products rate-limited per user (max 1 unit per userId) enforced at cart add time.
- **Cart Compression** — for users with large carts (50 items), store compressed JSON in Redis to reduce memory.

---

*Document prepared for Staff / Principal Engineer interviews. Deep-dive areas: concurrent write conflicts with optimistic locking, cache stampede prevention, checkout snapshot contract, multi-region active-active, and CQRS event model.*
