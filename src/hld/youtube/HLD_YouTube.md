# 📺 High-Level Design (HLD) — YouTube (Video Streaming Platform)
> **Target Level:** SDE 3 / Principal Engineer  
> **Interview Focus:** Video Ingestion Pipeline, Adaptive Streaming (HLS/DASH), CDN Architecture, Recommendation Engine, Search, Scalability

---

## 1. Requirements

### 1.1 Functional Requirements
- Users can **upload videos** (up to 12 hours / 256 GB raw).
- Videos are **processed and available** for streaming within minutes of upload.
- Users can **stream videos** in multiple resolutions (144p – 4K) with adaptive bitrate.
- Users can **search** for videos by title, description, channel, tags.
- **Personalized homepage** — ranked video recommendations.
- Users can **like, dislike, comment, subscribe** to channels.
- **Watch history** and **resume playback** across devices.
- Support **live streaming** with real-time chat.
- **Creator analytics** — views, watch time, revenue, audience retention.
- Support **captions / subtitles** (auto-generated + manual).

### 1.2 Non-Functional Requirements
| Property | Target |
|---|---|
| **Video startup latency** | < 2 sec to first frame |
| **Upload availability** | Video playable within 5 min of upload completion |
| **Streaming quality** | Adaptive bitrate; < 1% buffering events |
| **Availability** | 99.99% for streaming; 99.9% for uploads |
| **Scale** | 2B DAU, 500 hours of video uploaded/min, 1B hours watched/day |
| **Search latency** | < 200 ms |
| **Storage** | ~1 exabyte total video storage |

### 1.3 Out of Scope
- Ad serving / monetization pipeline
- Creator payment / revenue calculation
- YouTube Kids content filtering

---

## 2. Capacity Estimation

```
DAU                         = 2 billion
Uploads/min                 = 500 hours of video = 30,000 min of video/min
                            → ~3,600 videos/min uploaded (avg 8-min video)
Video size raw (8 min HD)   ≈ 1.5 GB
Uploads storage/day         = 3,600/min × 1,440 min × 1.5 GB = ~7.8 PB/day (raw)
After transcoding (multi-res) ≈ 5× compression → ~40 PB stored/day
Watch hours/day             = 1 billion hours = 60B min/day → ~700M GB/day of video served
CDN must serve              ≈ ~700 million GB/day ≈ ~8 TB/sec peak throughput
Comments/day                = 500 million
Search queries/day          = 3 billion → ~35K/sec
```

---

## 3. High-Level Architecture

```
 Creator (Upload)                    Viewer (Stream)
      │                                     │
      ▼                                     ▼
 ┌──────────────┐                  ┌────────────────────┐
 │  Upload      │                  │   API Gateway      │
 │  Service     │                  │   (Auth, RL)       │
 └──────┬───────┘                  └─────────┬──────────┘
        │                                    │
        ▼                           ┌────────┴────────────────────────┐
 ┌──────────────────────┐           ▼                                 ▼
 │  Raw Video Storage   │   ┌──────────────┐                ┌──────────────────┐
 │  (S3)                │   │  Video       │                │  Recommendation  │
 └──────┬───────────────┘   │  Service     │                │  Engine          │
        │                   │  (metadata,  │                └──────────────────┘
        ▼                   │  streaming   │
 ┌──────────────────────┐   │  URLs)       │                ┌──────────────────┐
 │  Video Processing    │   └──────┬───────┘                │  Search Service  │
 │  Pipeline (async)    │          │                         │  (Elasticsearch) │
 │  - Transcoding       │          ▼                         └──────────────────┘
 │  - Thumbnail gen     │   ┌──────────────┐
 │  - Subtitle gen      │   │  CDN         │  (served globally, ABR streaming)
 │  - Moderation        │   │  (CloudFront │
 └──────┬───────────────┘   │  / Akamai)   │
        │                   └──────────────┘
        ▼
 ┌──────────────────────┐
 │  Processed Video     │
 │  Storage (S3)        │
 │  Multi-res HLS/DASH  │
 └──────────────────────┘
        │
        ▼
 ┌──────────────────────┐
 │  Kafka Event Bus     │
 │  (VideoPublished,    │
 │   ViewEvent, Likes)  │
 └──────────────────────┘
```

---

## 4. Video Upload & Processing Pipeline

### 4.1 Upload Flow

```
Creator (YouTube Studio app)
        │
        ▼
Upload Service:
  1. Auth: validate creator account active
  2. Validate: file format (MP4, MOV, AVI, MKV...), size < 256 GB
  3. Issue pre-signed S3 URL (multipart upload for large files)
  4. Client uploads directly to S3 (bypasses YouTube servers)
     → Chunked upload: 8 MB chunks, resumable if interrupted
  5. S3 triggers SNS → SQS → VideoProcessingWorker
  6. Return videoId = Snowflake ID to creator
  7. Video status = "PROCESSING" — shown on YouTube Studio
```

**Resumable upload (critical for large files):**
```
Client: POST /upload/init → gets uploadSessionId + S3 multipart uploadId
Client: PUT /upload/{uploadSessionId}?partNumber=1 (8 MB chunk)
Client: PUT /upload/{uploadSessionId}?partNumber=2 ...
Client: POST /upload/{uploadSessionId}/complete → triggers S3 multipart complete
→ Survives network interruption; client resumes from last uploaded chunk
```

### 4.2 Video Processing Pipeline

```
Raw video arrives in S3
        │
        ▼
Kafka: VideoUploaded event
  → Processing Orchestrator (Temporal / AWS Step Functions)
        │
   ┌────┴──────────────────────────────────┐ (parallel workers)
   ▼             ▼              ▼          ▼
Transcoding  Thumbnail     Auto-Caption  Content
(FFmpeg)     Generation    (Speech-to-   Moderation
             (3 frames)    text AI)      (NSFW/Copyright)
   │
   ▼
Target resolutions (HLS + DASH output):
  4K (2160p) → ~20 Mbps
  1080p Full HD → ~8 Mbps
  720p HD → ~5 Mbps
  480p → ~2.5 Mbps
  360p → ~1 Mbps
  240p → ~0.5 Mbps
  144p → ~0.15 Mbps
        │
Each resolution → split into 10-second HLS chunks → stored in S3
        │
        ▼
S3 path: s3://yt-videos/{videoId}/{resolution}/{segment_N}.ts
         s3://yt-videos/{videoId}/manifest.m3u8  (master playlist)
        │
        ▼
CDN origin updated → video publicly streamable
Kafka: VideoProcessed → Search indexing, recommendation eligibility
Video status = "PUBLISHED"
Total Pipeline Time: ~3–5 min for 8-min HD video
```

**Why parallel workers?**
- Transcoding is CPU-bound; different resolutions processed simultaneously.
- 4K takes ~10 min per video; 360p takes 30 sec — don't let 4K block 360p.

### 4.3 Transcoding Worker Scaling (Staff-Level)

```
Challenge: 500 hours uploaded/min × ~5 GPU-minutes/video = 150,000 GPU-minutes/min at peak

Worker Fleet Architecture:
  Priority Queue (SQS FIFO with 3 tiers):
    P0 — Live stream ingest     (real-time SLA: <2 sec chunk delay)
    P1 — Premium/Partner creators (SLA: <2 min to first playable resolution)
    P2 — Standard creators       (SLA: <5 min; best-effort)

Auto-Scaling Policy:
  Metric:  SQS ApproximateNumberOfMessages per tier
  Scale-out: +50 GPU workers when P1 queue depth > 200 messages
  Scale-in:  -10 workers every 5 min when queue < 20 messages
  Min fleet: 500 workers (always-on On-Demand) — handles baseline load
  Burst fleet: up to 10,000 Spot instances (g4dn.xlarge for H.264, g5 for AV1)

Cost control:
  On-Demand instances: P0 + P1 only (SLA-critical)
  Spot instances: P2 standard uploads — ~70% cost saving
  Spot interruption handling:
    Each resolution is an independent idempotent task → save progress per segment
    If Spot interrupted, re-queue only incomplete segments (not full video)

Worker checkpointing:
  Worker writes: s3://yt-processing/{videoId}/progress.json
    { "completedSegments": [0,1,2,..N], "resolution": "720p", "workerId": "w-abc" }
  On re-assignment: new worker skips already-written segments → idempotent

Early playback (progressive availability):
  360p finishes in ~30 sec → video status = PARTIALLY_AVAILABLE
  Creator and viewers can watch at 360p immediately
  Higher resolutions published as they complete
  Prevents 5-min wait for full 4K processing
```

---

## 5. Adaptive Bitrate Streaming (ABR)

**The key to smooth playback on any network.**

### 5.1 HLS (HTTP Live Streaming) — Apple/Mobile

```
Master Playlist (manifest.m3u8):
  #EXT-X-STREAM-INF:BANDWIDTH=8000000,RESOLUTION=1920x1080
  1080p/playlist.m3u8

  #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1280x720
  720p/playlist.m3u8

  #EXT-X-STREAM-INF:BANDWIDTH=1000000,RESOLUTION=640x360
  360p/playlist.m3u8

Individual resolution playlist (720p/playlist.m3u8):
  #EXTINF:10.0,
  segment_0.ts
  #EXTINF:10.0,
  segment_1.ts
  ...
```

### 5.2 Client ABR Logic

```
Client (video player) monitors buffer health every 2 sec:

Buffer Level    → Action
> 30 sec        → upgrade to next higher resolution
10–30 sec       → maintain current resolution
5–10 sec        → downgrade one resolution
< 5 sec         → immediately drop to lowest available
< 2 sec         → buffering event (user sees spinner)

Network estimation:
  Measure bandwidth of each segment download → rolling average
  Estimated bandwidth < 5 Mbps → cap at 720p
  Estimated bandwidth < 1.5 Mbps → cap at 360p
```

### 5.3 Video Serving via CDN

```
Client → CDN Edge PoP (nearest geographic node)
          │
          Cache HIT (>90% for popular videos) → serve immediately
          │
          Cache MISS → fetch from S3 origin, cache at edge (TTL: 24hr)

CDN strategy:
  - Top 1M most-watched videos: pre-pushed to all PoPs globally (~200 PoPs)
  - Long-tail: fetched from origin on first request per region, cached locally
  - Segments served with Range header support (partial content for seeking)

Seek optimization:
  User skips to 5:30 → client fetches segment containing 5:30
  Pre-buffers next 3 segments from that point
  No need to re-download earlier segments
```

### 5.4 CDN Invalidation & Video Takedown (Staff-Level)

```
Problem: Video deleted/taken-down, but CDN edges still serve cached .ts segments.
         Naive approach (wait for TTL) = 24-hr window where banned content is accessible.

Solution: Signed URL tokens + CDN purge API

Signed URL approach (preferred at scale):
  Every CDN URL includes a signed token:
    https://cdn.yt.com/{videoId}/720p/seg_0.ts?token=<HMAC>&expires=<ts>&policy=<base64>
  Token signed by Video Service → verified at CDN edge (Lambda@Edge / Akamai EdgeAuth)
  On takedown:
    1. Set video status = REMOVED in DB (< 100 ms)
    2. Revoke signing key for this videoId in token auth service
    3. All new segment requests → 403 Forbidden (token invalid)
    4. No CDN purge needed — existing cached bytes are now unservable

CDN Purge API (fallback for non-signed assets like thumbnails):
  Bulk purge: DELETE /cdn/invalidate { paths: ['/thumb/{videoId}/*'] }
  CloudFront: CreateInvalidation API — propagates to all PoPs in ~1-2 min
  Akamai: Fast Purge API — ~5 sec global propagation
  SLA for takedown: <2 min end-to-end (DB update + key revocation + purge)

Segment TTL strategy:
  Normal video segments:  TTL = 24 hr (high cache efficiency)
  Recently uploaded:      TTL = 5 min (creator may still edit metadata)
  Live stream chunks:     TTL = 2 sec (freshness requirement)
  Manifests (m3u8):       TTL = 30 sec (dynamic; resolution availability changes)
```

---

## 6. Core Services

### 6.1 Video Metadata Service

```sql
-- PostgreSQL (relational — video has many relationships)
CREATE TABLE videos (
    video_id        BIGINT PRIMARY KEY,    -- Snowflake ID
    channel_id      UUID NOT NULL,
    title           TEXT,
    description     TEXT,
    tags            TEXT[],
    duration_sec    INT,
    status          TEXT,   -- PROCESSING|PUBLISHED|DELETED|PRIVATE
    visibility      TEXT,   -- PUBLIC|UNLISTED|PRIVATE
    category        TEXT,
    default_lang    TEXT,
    view_count      BIGINT,
    like_count      INT,
    dislike_count   INT,
    thumbnail_url   TEXT,
    published_at    TIMESTAMP
);

CREATE TABLE video_streams (
    video_id        BIGINT REFERENCES videos,
    resolution      TEXT,   -- '1080p','720p','360p'...
    bitrate_kbps    INT,
    hls_url         TEXT,   -- CDN URL for m3u8 playlist
    file_size_bytes BIGINT,
    PRIMARY KEY (video_id, resolution)
);

CREATE TABLE video_captions (
    video_id  BIGINT,
    lang_code TEXT,
    vtt_url   TEXT,         -- CDN URL for WebVTT caption file
    PRIMARY KEY (video_id, lang_code)
);
```

**Hot metadata cached in Redis:**
```
Key:   video:{videoId}
Type:  HASH
Fields: title, channelId, duration, thumbnailUrl, viewCount, likeCount, hlsUrl
TTL:   1 hr (refreshed on view events)
```

### 6.2 View Count & Engagement Tracking

```
Problem: 1B views/day = ~11,500 view events/sec
         Direct DB writes → kills PostgreSQL

Solution: Approximate counting with eventual consistency

Client → POST /view { videoId, watchDurationSec, percentWatched }
        │
        ▼
Kafka topic: view-events  (partitioned by videoId)
        │
        ▼
Flink streaming job (1-min tumbling window):
  Aggregate view counts → batch INCRBY to Redis:
  INCRBY viewcount:{videoId} N
        │
        ▼
Hourly DB sync job:
  Flush Redis counters → UPDATE videos SET view_count = view_count + N

Likes / Dislikes:
  Redis SET per user (dedup): liked:{userId}:{videoId}
  INCR/DECR like_count:{videoId}
  Synced to DB every 5 min
```

### 6.3 Search Service

```
Elasticsearch index: videos
  Indexed fields: title (boost=3), description (boost=1), tags (boost=2),
                  channel_name (boost=2), transcript (boost=0.5)

Query: "machine learning tutorial python 2024"
  → Multi-match across indexed fields
  → Score = text_relevance × popularity_score × recency_factor

Ranking signals:
  - View count velocity (views in last 7 days)
  - Watch time (avg % watched — quality signal)
  - Like/dislike ratio
  - Channel authority (subscriber count)
  - Upload recency

Indexing pipeline:
  Kafka (VideoProcessed) → ES consumer → indexed in < 60 sec

Autocomplete:
  Redis ZSET per prefix (see Search Autocomplete HLD)
```

### 6.4 Recommendation Engine

**YouTube's recommendation is the most sophisticated in the world — drives 70% of watch time.**

**Two-stage pipeline (same as described in our other HLDs):**

```
Stage 1: Candidate Generation (~1,000 videos)
  Sources:
  a) Collaborative filtering: users with similar watch history liked these
  b) Content-based: similar to recently watched (title, tags, category embedding)
  c) Subscriptions: latest videos from subscribed channels
  d) Trending (globally + per category + per location)
  e) Previously interrupted videos (resume watching)

Stage 2: Ranking (1,000 → top 20 homepage cards)
  Deep Neural Network (YouTube's DNN Ranking paper, 2016):
  Features:
    User: watch history embeddings, search history, demographics, location
    Video: view count, like ratio, avg watch %, upload recency, category
    Context: device, time of day, previous video in session
  Target: Maximize: P(watch > 50%) × watch_duration + P(like) + P(subscribe)

Stage 3: Post-processing
  - Diversity: no 3 consecutive videos from same channel
  - Freshness boost: at least 2 videos < 24 hr old on homepage
  - Filter: already-watched videos (optional, configurable)
  - Safe mode filtering for restricted users
```

**Watch history → embedding:**
```
Each video encoded as 256-dim embedding via Word2Vec-style training
User embedding = avg(embeddings of last 50 watched videos)
→ Enables fast ANN lookup for candidate generation
```

**ANN Infrastructure (Staff-Level):**
```
Vector index (candidate generation at scale):
  Library:  Google ScaNN (YouTube uses internally) or FAISS (open-source)
  Index:    ~50M video embeddings (256-dim float32) ≈ 50M × 256 × 4 bytes ≈ 51 GB
  Sharding: Index sharded across 20 servers; each handles a partition of video space
  Query:    User embedding broadcast to all shards → top-K per shard → merge → top-1000
  Latency:  <10 ms for candidate generation at P99

Embedding freshness:
  Video embeddings:  Recomputed nightly (Spark batch) for all active videos
  User embeddings:   Two layers:
    a) Long-term:   Spark batch daily — full watch history, stable preferences
    b) Session:     Flink real-time — updates every 3 videos watched in current session
                    Weighted: session_embedding × 0.6 + long_term × 0.4
  Effect: User who just watched 3 cooking videos → session shifts toward food content

Cold Start Problem:
  New User (0 watch history):
    Phase 1 (0-3 videos): Show "Trending" + "Top in category" (geography-based)
    Phase 2 (3-10 videos): Collaborative filtering on interest categories
                           (selected at signup or inferred from first watches)
    Phase 3 (10+ videos):  Full DNN pipeline kicks in

  New Video (just published):
    No engagement signals yet → use content-based embedding only
    Serve to small cohort matching content embedding → collect early CTR + watch %
    After 1,000 views: engagement signals incorporated into ranking score
    After 10,000 views: full ranking pipeline (CTR × watch_duration objective)
    "Exploration traffic": 5% of impressions reserved for new/unproven videos
```

### 6.5 Watch History & Resume Playback

```
On every 10 sec of playback:
  Client sends: PATCH /history { videoId, watchPosition: 324, percentWatched: 45 }
    → Kafka: WatchProgress event
    → Flink: upsert to watch_history (last position per user per video)

Resume playback:
  GET /video/{videoId}/context?userId=U123
  → Returns: { lastPosition: 324, percentWatched: 45 }
  → Player seeks to 324 sec on load

Watch history stored in:
  Redis: history:{userId} → ZSET of {videoId: last_watch_timestamp} (last 200 videos)
  DynamoDB: long-term history (userId, videoId) → full record
  TTL: Redis 30 days; DynamoDB permanent (user-controlled clear)
```

### 6.6 Live Streaming

```
Creator starts live stream:
  → OBS / YouTube Studio → RTMP stream to Ingest Server
  → Ingest server → transcodes to HLS (near real-time, ~2-4 sec latency)
  → HLS chunks (.ts) → S3 → CDN
  → Viewers fetch latest HLS chunks with short TTL (2 sec freshness)

Live chat:
  → WebSocket connections to Chat Service
  → Each message → Kafka → fan-out to all chat subscribers in room
  → Redis: chat:{liveVideoId} → active WebSocket sessions
  → Rate limit: 1 message / 3 sec per user (prevent spam)

Super Chats (paid messages):
  → Payment flow → prioritized pinned display in chat
```

**Live Streaming Resilience (Staff-Level):**
```
Ingest Server Failover:
  Creator's OBS pushes RTMP to primary ingest server
  Ingest servers maintain heartbeat to health coordinator
  If primary ingest fails:
    DNS TTL = 5 sec on ingest endpoint → quick failover
    OBS client auto-reconnects to same URL → load balancer routes to healthy ingest
    Viewer-side: HLS player retries last successful segment timestamp
    Gap in chunks: player stalls for max 10 sec → shows "Reconnecting..."
    Segments resume from where ingest server left off (no rewind)

DVR / Replay Buffer:
  All live HLS chunks retained in S3 for 12 hours after stream ends
  Viewer can seek back up to 4 hours in "live" mode (configurable)
  VOD replay: after stream ends, manifest recombined into full-length VOD
  Processing pipeline runs async to generate standard multi-res VOD version

Low-Latency Live Streaming (LL-HLS):
  Standard HLS: 6–30 sec latency (3 × 10-sec segments in buffer)
  LL-HLS (HTTP/2 Push):  2–3 sec latency
    Segments: 0.5–2 sec instead of 10 sec
    Server push: CDN pushes next partial segment before client requests it
    Use case: live sports, live events where latency matters
  Trade-off: 5× more CDN requests per viewer-minute; higher edge compute cost
  Decision: LL-HLS opt-in for creators; standard HLS default

Live Chat Fan-out at Scale (100K concurrent viewers):
  Naive: Fan out each message to 100K WebSocket connections → O(N) per message
  Solution: Tiered fan-out
    Chat Server writes message → Kafka topic: livechat:{videoId}
    Kafka partitioned by videoId → N consumers, each owning a subset of WS connections
    Each consumer responsible for ~5,000 connections → 20 consumer instances at 100K scale
    Redis PubSub used within a consumer node for in-process fan-out
  Spam control:
    Slow mode: host-configurable delay between messages per user (0–300 sec)
    Profanity filter: pre-publish regex + ML classifier (<5 ms, P99)
    Rate limit: 1 msg/3 sec per user enforced at chat service (Redis token bucket)
```

---

## 7. Content Moderation Pipeline

```
On video upload:
  1. Copyright detection:
     Content ID: audio/video fingerprinting against rights-holder library
     If match → auto-mute / block / revenue share per policy

  2. NSFW / violence detection:
     ML classifier on video frames (sampled every 2 sec)
     Score > 0.9 safe → PUBLISHED
     Score 0.5–0.9 → human review queue (SLA: 24 hr)
     Score < 0.5 → AUTO_REMOVED

  3. Spam/misleading metadata:
     NLP classifier on title + description + tags

  4. Community guidelines violations:
     Post-publish: user reports → human review → strike / removal
```

**Content ID / Copyright Fingerprinting Deep Dive (Staff-Level):**
```
Challenge: Detect if uploaded video contains copyrighted audio or video
           even if re-encoded, pitch-shifted, sped-up, or partially clipped.

Audio Fingerprinting:
  Algorithm: Chromaprint (used by AcoustID) or Shazam-style hashing
  Process:
    1. Extract MFCC (Mel-Frequency Cepstral Coefficients) from audio stream
    2. Generate 32-bit fingerprint hash per 0.5-sec window → sequence of hashes
    3. Compare against Content ID DB (billions of fingerprint sequences)
    4. Match = sliding window comparison: 30+ consecutive matching hashes → flag
  Robustness: survives 10% speed change, re-encoding, minor pitch shifts

Video Fingerprinting:
  Algorithm: Perceptual hashing (pHash) on keyframes
  Process:
    1. Extract 1 keyframe/sec → resize to 32×32 grayscale
    2. DCT (Discrete Cosine Transform) → take top-left 8×8 coefficients
    3. 64-bit pHash per frame
    4. Compare Hamming distance against reference pHash sequences
    5. Match: Hamming distance < 10 bits across 30 consecutive frames
  Survives: re-encoding, mild crop, letterboxing, color grading
  Does NOT survive: heavy cropping, mirroring + speed change combo

Content ID DB Scale:
  Rights-holders upload reference tracks → fingerprints pre-computed + stored
  Storage: ~50B fingerprint hashes (audio + video combined) ≈ few TB (compact index)
  Lookup: LSH (Locality Sensitive Hashing) for approximate nearest-neighbor match
  Latency SLA: full fingerprint scan < 60 sec per uploaded video

Policy Engine:
  Rights-holder sets policy per asset:
    BLOCK      → video removed immediately
    MONETIZE   → ads run; revenue split to rights-holder
    TRACK      → video stays, rights-holder gets view analytics only
  Multiple claimants: most restrictive policy wins

Timeline:
  T+0:   Upload complete → video status = PROCESSING
  T+30s: 360p available → PARTIALLY_AVAILABLE (before Content ID scan)
  T+60s: Content ID scan complete → if BLOCK → take down; else continue
  T+5min: Full pipeline done → PUBLISHED
  Note: video is publicly accessible during 30–60 sec window before Content ID
        → accepted trade-off for user experience ("video ready fast")
```

---

## 8. CDN & Storage Architecture

```
Storage tiers:
  HOT   (< 30 days, frequent access) → S3 Standard
  WARM  (30–180 days, occasional)    → S3 Standard-IA
  COLD  (> 180 days, rare)           → S3 Glacier
  ARCHIVE (> 5 years)                → S3 Deep Archive

Cost optimization:
  Top 1% of videos = 95% of watch time → keep in HOT tier
  Long-tail: auto-tiered based on access frequency
  CDN caches top videos at edge → reduces S3 egress costs significantly

CDN PoP strategy:
  200+ global PoPs (CloudFront / Akamai)
  US: 40 PoPs, Europe: 30 PoPs, India: 15 PoPs, etc.
  Popular videos pre-warmed to all PoPs on publish
  Live streams: special low-latency edge nodes
```

---

## 9. Scalability

| Layer | Strategy |
|---|---|
| **Upload** | S3 multipart; pre-signed URLs; bypass app servers |
| **Processing** | Auto-scaling transcoding workers (GPU instances for 4K) |
| **CDN** | 90%+ of video bytes served from edge; transparent to origin |
| **Metadata reads** | Redis cache (1hr TTL); < 5% DB reads |
| **View counts** | Kafka + Flink batching; avoid per-view DB write |
| **Search** | Elasticsearch sharded by content category |
| **Recommendations** | Pre-computed per user; daily Spark batch + real-time session update |
| **Live chat** | Redis PubSub + horizontal WebSocket servers |

---

## 10. Key Design Decisions & Trade-offs

| Decision | Choice | Trade-off |
|---|---|---|
| **Upload** | Pre-signed S3 + resumable multipart | No YouTube server bandwidth used; complex client retry logic |
| **Processing** | Async pipeline (Kafka + workflow engine) | Video not instant; ~5 min delay acceptable |
| **Streaming format** | HLS + DASH | Maximum device compatibility; playlist overhead |
| **CDN** | Multi-PoP global edge | 90%+ cache hit; cache invalidation complexity on deletion |
| **View counts** | Eventually consistent (Kafka → Redis → DB sync) | Slight undercounting; ~minutes to hours delay is fine |
| **Recommendations** | Two-stage DNN (candidate gen + ranking) | Best quality; requires significant ML infrastructure |
| **Storage tiering** | S3 Standard → IA → Glacier by age | 10× cost reduction for long-tail old videos |
| **ABR algorithm** | Buffer-based (BOLA) | Minimizes rebuffering; slight quality oscillation |

---

## 11. Monitoring & Observability

| Metric | Alert |
|---|---|
| **Video startup time** | P99 > 3 sec |
| **Buffering ratio** | > 1% of playback sessions |
| **Processing pipeline lag** | Video > 10 min in PROCESSING state |
| **CDN cache hit rate** | < 85% |
| **Upload failure rate** | > 2% |
| **Copyright false positive rate** | Content ID blocking legitimate content |
| **Live stream ingest lag** | > 6 sec (viewer latency) |

---

## 12. Database Sharding Strategy (Staff-Level)

```
Problem: videos table with billions of rows cannot fit on a single PostgreSQL instance.

Sharding Key Choice:
  Option A: Shard by video_id (hash-based)
    Pros: Uniform distribution; hot video not on same shard
    Cons: Channel's videos spread across shards → listing all videos per channel = scatter-gather

  Option B: Shard by channel_id (hash-based)
    Pros: All videos for a channel on one shard → fast channel page queries
    Cons: Viral channels (MrBeast: 300M subscribers) → hot shard problem

  Decision: Shard by video_id (hash mod N)
    Channel page uses a separate channel_videos index service (Elasticsearch or Cassandra)
    Hot shard risk is eliminated (video_id uniform distribution)

Sharding Architecture:
  16 logical shards → mapped to 4 physical shard groups (4 shards per Postgres cluster)
  Each shard group: 1 primary + 2 read replicas
  Routing: shard = consistent_hash(video_id) % 16
  Router layer (Vitess or PgBouncer + routing middleware) hides shard topology from services

Read Replica Routing:
  Video detail page:       Read from replica (eventual consistency OK; metadata stable)
  Creator Studio update:   Write to primary → read-your-own-writes (sticky session to primary)
  Admin/moderation tools:  Always read from primary (strong consistency required)
  Replica lag SLO: < 100 ms (async replication; Postgres streaming replication)

listing_calendar equivalent (video_streams table):
  Sharded same as videos (by video_id)
  Archive strategy:
    Videos older than 2 years with < 1,000 views/month → move to cold shard (Postgres on cheaper storage)
    Metadata kept; streams pointer updated to cold shard → transparent to API

Comments Table:
  Volume: 500M comments/day → cannot co-locate with video metadata
  Separate Cassandra cluster: partition_key = video_id, clustering_key = comment_id DESC
  Cassandra chosen for: high write throughput, infinite horizontal scale, time-range queries per video
  Top comments: pre-computed and cached in Redis (refreshed every 5 min)
```

---

## 13. Multi-Region & Disaster Recovery (Staff-Level)

### 13.1 Regional Topology

```
Regions: US-EAST-1 (primary), EU-WEST-1, AP-SOUTHEAST-1, IN-SOUTH-1

Service distribution:
  Upload Service:       Active in all regions → uploads land in nearest region's S3
                        Cross-region S3 replication → global processing fleet picks up job
  Video Processing:     Central fleet in US-EAST-1 (cost + GPU availability)
                        Regional processing for latency-sensitive live streams
  Streaming CDN:        Fully distributed — all 200+ PoPs globally serve from edge
  Metadata DB:          Primary in US-EAST-1; async replicas in EU + APAC
                        Read replicas serve local reads (<10 ms vs ~150 ms cross-continent)
  Recommendation:       Pre-computed results stored in regional Redis clusters
                        Replication: US primary → EU + APAC via Kafka geo-replication
  Search (ES):          Per-region ES clusters; index synced from US primary via Kafka
```

### 13.2 Active-Active vs. Active-Passive

```
Streaming (read path):   Active-Active — every region serves content independently
                          No coordination needed; CDN is stateless

Upload (write path):     Active-Active with regional affinity
                          Upload → nearest region S3 → processing job queued
                          video_id generated by Snowflake (machine_id encodes region)
                          No cross-region write coordination for uploads

Metadata writes:         Active-Passive (US-EAST-1 is primary)
                          Reason: avoid split-brain on video status (PUBLISHED/REMOVED)
                          EU/APAC creators accept ~150 ms extra write latency
                          Alternative: CRDTs for non-critical fields (like_count)

Booking-equivalent (reservations → video publish state):
  Video publish is a one-way idempotent transition (PROCESSING → PUBLISHED)
  Idempotency key = video_id → safe to retry across regions
```

### 13.3 Failure Modes & Recovery

```
Scenario 1: Primary DB (US-EAST-1) fails
  Detection: Health check fails → PagerDuty alert in < 30 sec
  Failover:  Promote EU-WEST-1 read replica to primary (automated, RDS Multi-AZ: <60 sec)
  Impact:    Uploads queue in regional S3; processing pauses; streaming unaffected
  RPO:       < 5 sec (async replication lag target)
  RTO:       < 2 min for metadata writes

Scenario 2: Processing pipeline outage
  Videos queue in S3 / SQS; no data loss
  Backlog processed when pipeline recovers (priority queue drains P0 first)
  Creator shown "Processing taking longer than expected" after 10-min SLA breach

Scenario 3: CDN PoP failure (single region)
  DNS-based failover: Anycast routing moves traffic to nearest healthy PoP
  CDN provider SLA: <30 sec PoP-level failover
  Impact: Viewers in affected region see slightly higher latency (cache miss → origin)

Scenario 4: Full region loss (US-EAST-1)
  Streaming: CDN serves from edge; fully resilient — no origin hit needed for cached content
  Uploads: DNS failover to EU-WEST-1 upload endpoint
  Processing: EU processing fleet activates; uses S3 cross-region replication as input
  Metadata: EU replica promoted; ~5 sec of writes potentially lost (RPO)
  RTO:  <15 min for full upload+processing path restoration
  RPO:  <5 sec for metadata; 0 for streaming (CDN stateless)

RTO / RPO Summary:
  | Path              | RTO        | RPO       |
  |---                |---         |---        |
  | Streaming (CDN)   | ~0 sec     | 0 sec     |
  | Uploads           | < 2 min    | 0 (queue) |
  | Video processing  | < 5 min    | 0 (queue) |
  | Metadata reads    | < 1 min    | < 5 sec   |
  | Metadata writes   | < 2 min    | < 5 sec   |
```

### 13.4 Data Durability

```
Raw video S3:      Replication factor 3 within region + Cross-region replication
Processed video:   Same as raw + CDN acts as additional distributed cache
Metadata DB:       WAL-based replication; PITR backup retained 30 days
Kafka events:      Retention 7 days; replayable for re-indexing or audit
Checksums:         S3 ETag (MD5) verified on every multipart upload complete
```

---

## 14. Future Enhancements
- **YouTube Shorts** — separate vertical video feed optimized for mobile (< 60 sec, TikTok-style FYP).
- **8K / 360° / VR streaming** — requires specialized encoding and player.
- **AI video chapters** — auto-detect scene changes → generate chapter titles from transcript.
- **Offline download** (Premium) — DRM-encrypted download similar to Spotify's model.
- **Real-time translation** — live dubbing of videos into user's language via AI.

---

*Document prepared for SDE 3 system design interviews. Focus areas: video processing pipeline (FFmpeg + HLS), adaptive bitrate streaming, CDN architecture, two-stage recommendation (DNN), view count batching, live streaming, and S3 storage tiering.*
