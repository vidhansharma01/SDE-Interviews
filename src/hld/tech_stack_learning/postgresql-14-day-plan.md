# 🐘 PostgreSQL — 14-Day Deep-Dive Learning Plan

> **Target Audience:** Developers with basic SQL knowledge who want to master PostgreSQL at a production/SDE 3 level.
> **Goal:** Move from fundamentals to advanced internals, performance tuning, and high availability design.

---

## 📅 Overview

| Day | Topic | Difficulty |
|-----|-------|------------|
| 1 | Installation, Architecture & psql CLI | 🟢 Beginner |
| 2 | Data Types, DDL & Schema Design | 🟢 Beginner |
| 3 | CRUD, Filtering & Operators | 🟢 Beginner |
| 4 | Joins, Subqueries & Set Operations | 🟡 Intermediate |
| 5 | Aggregations, Window Functions & CTEs | 🟡 Intermediate |
| 6 | Indexes — Types, Internals & Strategy | 🟡 Intermediate |
| 7 | Transactions, MVCC & Isolation Levels | 🟡 Intermediate |
| 8 | Stored Procedures, Functions & Triggers | 🟡 Intermediate |
| 9 | Query Planner & EXPLAIN ANALYZE | 🔴 Advanced |
| 10 | Partitioning & Table Inheritance | 🔴 Advanced |
| 11 | Replication — Streaming, Logical & HA | 🔴 Advanced |
| 12 | JSON/JSONB, Full-Text Search & Extensions | 🔴 Advanced |
| 13 | Security, Roles, RLS & Auditing | 🔴 Advanced |
| 14 | Performance Tuning, Vacuuming & Production Ops | 🔴 Advanced |

---

## Day 1 — Installation, Architecture & psql CLI

### 1.1 What is PostgreSQL?

PostgreSQL (pronounced *post-gres-Q-L*) is a powerful, open-source **object-relational database management system (ORDBMS)**. It was born from the POSTGRES project at UC Berkeley in 1986 and became open source in 1994. It supports:

- Full ACID compliance
- Advanced data types (arrays, JSON, hstore, geometric)
- Extensibility (custom types, operators, index methods)
- MVCC (Multi-Version Concurrency Control) for non-blocking reads
- Standards-compliant SQL with many extensions

### 1.2 PostgreSQL Process Architecture

Understanding the process model is key to understanding performance and troubleshooting.

```
Client Application
       |
       | (TCP or Unix socket)
       |
  Postmaster (pg_ctl, pid in postmaster.pid)
       |
       |--- Backend Process (one per connection)
       |         - Parses SQL
       |         - Plans query (planner/optimizer)
       |         - Executes query
       |         - Manages memory (work_mem)
       |
       |--- Background Workers:
             - WAL Writer       → flushes WAL to disk
             - Checkpointer     → writes dirty pages to disk
             - Background Writer → proactively writes dirty pages
             - Autovacuum Launcher → spawns vacuum workers
             - Stats Collector  → pg_stat_* views
             - Logical Replication Workers
```

**Key Memory Areas:**
- `shared_buffers` — Shared cache of data pages (like buffer pool in Oracle/MySQL). Default 128MB, recommend 25% of RAM.
- `work_mem` — Per-sort/hash operation memory. Each query can use multiple. Be careful with high connection counts.
- `maintenance_work_mem` — For VACUUM, CREATE INDEX, ALTER TABLE.
- `wal_buffers` — Buffer for WAL records before writing to disk.

### 1.3 Key Files & Directories

```bash
$PGDATA/                     # Data directory (e.g., /var/lib/postgresql/15/main)
├── base/                    # Database files (one subdirectory per OID)
│   └── 16384/               # Default database (postgres)
│       ├── 1259             # pg_class heap file
│       └── 1259_fsm         # Free Space Map
├── global/                  # Cluster-wide tables (pg_database, pg_roles)
├── pg_wal/                  # Write-Ahead Log segments (16MB each by default)
├── pg_xact/                 # Transaction commit log (CLOG)
├── postgresql.conf          # Main configuration
├── pg_hba.conf              # Client authentication rules
└── PG_VERSION               # PostgreSQL version
```

### 1.4 Installing PostgreSQL

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib

# Start and enable
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Switch to postgres user
sudo -i -u postgres
psql
```

**macOS (Homebrew):**
```bash
brew install postgresql@15
brew services start postgresql@15
psql postgres
```

**Docker (recommended for learning):**
```bash
docker run --name pg-learn \
  -e POSTGRES_PASSWORD=secret \
  -e POSTGRES_DB=learndb \
  -p 5432:5432 \
  -d postgres:15

docker exec -it pg-learn psql -U postgres -d learndb
```

### 1.5 Essential psql Commands

```sql
-- Meta-commands (no semicolon needed)
\l              -- List all databases
\c dbname       -- Connect to database
\dt             -- List tables in current schema
\dt schema.*    -- List tables in specific schema
\d tablename    -- Describe table (columns, indexes, constraints)
\d+ tablename   -- Detailed description
\di             -- List indexes
\dv             -- List views
\df             -- List functions
\du             -- List roles/users
\dn             -- List schemas
\timing         -- Toggle query timing
\e              -- Open editor for query
\i file.sql     -- Execute SQL file
\copy           -- Client-side COPY (vs server-side COPY)
\set VAR value  -- Set psql variable
\echo :VAR      -- Print variable
\x              -- Toggle expanded output (good for wide tables)
\q              -- Quit

-- Useful shortcuts
\pset pager off    -- Disable pager (less)
\pset null '[NULL]' -- Show NULLs explicitly
```

### 1.6 Connection String Format

```
postgresql://[user[:password]@][host][:port][/dbname][?param=value&...]

# Examples:
postgresql://postgres:secret@localhost:5432/learndb
postgresql://postgres@/learndb           # Unix socket
postgresql:///learndb                    # Default user, Unix socket
```

---

## Day 2 — Data Types, DDL & Schema Design

### 2.1 PostgreSQL Type System

PostgreSQL's type system is one of its strongest features. Every value has a type, and you can create custom types.

#### Numeric Types

```sql
-- Integer types
SMALLINT        -- 2 bytes, -32768 to 32767
INTEGER / INT   -- 4 bytes, -2B to 2B
BIGINT          -- 8 bytes, -9.2×10^18 to 9.2×10^18

-- Serial (auto-increment) — shorthand for sequence + default
SERIAL          -- INTEGER with auto-generated sequence
BIGSERIAL       -- BIGINT with auto-generated sequence
-- Modern preferred approach: GENERATED ALWAYS AS IDENTITY

-- Floating point (inexact)
REAL            -- 4 bytes, 6 decimal digits precision
DOUBLE PRECISION -- 8 bytes, 15 decimal digits precision
-- WARNING: Never use for money! 0.1 + 0.2 ≠ 0.3

-- Exact decimal (for money/finance)
NUMERIC(precision, scale)  -- Up to 131072 digits before decimal
DECIMAL(10, 2)             -- Same as NUMERIC; e.g. 99999999.99
MONEY                      -- Fixed-point, locale-dependent (avoid in new code)
```

#### Character Types

```sql
CHAR(n)         -- Fixed-length, blank-padded. Rarely useful.
VARCHAR(n)      -- Variable-length with limit
TEXT            -- Unlimited variable-length (preferred in PostgreSQL)
-- Performance: TEXT and VARCHAR are identical internally. Use TEXT.
```

#### Date & Time Types

```sql
DATE            -- 4 bytes. 4713 BC to 5874897 AD
TIME            -- Time of day without timezone
TIMETZ          -- Time with timezone (rarely useful, use TIMESTAMPTZ)
TIMESTAMP       -- Date + time without timezone
TIMESTAMPTZ     -- Date + time WITH timezone (stored as UTC internally)
INTERVAL        -- Time span: '1 year 2 months 3 days'

-- Always use TIMESTAMPTZ for application timestamps!
-- PostgreSQL stores UTC, displays in session timezone.
```

#### Boolean

```sql
BOOLEAN         -- TRUE / FALSE / NULL
-- Accepts: true, false, 't', 'f', 'yes', 'no', 'on', 'off', '1', '0'
```

#### Binary & Special

```sql
BYTEA           -- Binary data (escaped or hex format)
UUID            -- 128-bit UUID. Use gen_random_uuid() (pgcrypto/pg 13+)
JSON            -- Stores JSON as text (validates on insert)
JSONB           -- Binary JSON (decomposed, indexed, preferred!)
XML             -- XML data with validation
```

#### Arrays

```sql
-- Any type can be an array
INTEGER[]       -- Array of integers
TEXT[]          -- Array of text

CREATE TABLE tags_example (
    id SERIAL PRIMARY KEY,
    tags TEXT[]
);
INSERT INTO tags_example (tags) VALUES (ARRAY['postgresql','database','sql']);
SELECT * FROM tags_example WHERE 'postgresql' = ANY(tags);
```

#### Range Types

```sql
-- Built-in range types
INT4RANGE       -- Range of integers
INT8RANGE       -- Range of bigints
NUMRANGE        -- Range of numerics
TSRANGE         -- Range of timestamps (no tz)
TSTZRANGE       -- Range of timestamps (with tz) — use this!
DATERANGE       -- Range of dates

-- Example: Store event duration
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    name TEXT,
    duration TSTZRANGE
);
INSERT INTO events (name, duration)
VALUES ('Conference', '[2025-06-01, 2025-06-03)');
-- '(' = exclusive, '[' = inclusive

-- Find overlapping events (&&  operator)
SELECT * FROM events
WHERE duration && '[2025-06-02, 2025-06-04)';
```

### 2.2 DDL — Defining Your Schema

#### CREATE TABLE

```sql
CREATE TABLE users (
    -- Identity (preferred over SERIAL in PG 10+)
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Required fields
    email       TEXT        NOT NULL,
    username    TEXT        NOT NULL,
    password_hash TEXT      NOT NULL,

    -- Optional with defaults
    full_name   TEXT,
    avatar_url  TEXT,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    role        TEXT        NOT NULL DEFAULT 'viewer'
                            CHECK (role IN ('admin', 'editor', 'viewer')),

    -- Timestamps (always use TIMESTAMPTZ)
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ,                        -- soft delete

    -- Constraints
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_username_unique UNIQUE (username),
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$')
);
```

#### ALTER TABLE

```sql
-- Add column
ALTER TABLE users ADD COLUMN phone TEXT;

-- Rename column
ALTER TABLE users RENAME COLUMN phone TO phone_number;

-- Change type (careful with production data!)
ALTER TABLE users ALTER COLUMN phone_number TYPE VARCHAR(20);

-- Set default
ALTER TABLE users ALTER COLUMN is_active SET DEFAULT TRUE;

-- Add constraint
ALTER TABLE users ADD CONSTRAINT users_phone_unique UNIQUE (phone_number);

-- Drop constraint
ALTER TABLE users DROP CONSTRAINT users_phone_unique;

-- Make column NOT NULL (requires no existing NULLs)
ALTER TABLE users ALTER COLUMN phone_number SET NOT NULL;
```

#### Schemas

```sql
-- Schemas are namespaces within a database
CREATE SCHEMA app;
CREATE SCHEMA analytics;
CREATE SCHEMA audit;

-- Create table in schema
CREATE TABLE app.products (id SERIAL PRIMARY KEY, name TEXT);

-- search_path controls which schemas are searched
SET search_path TO app, public;

-- Grant usage
GRANT USAGE ON SCHEMA app TO appuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA app TO appuser;
```

### 2.3 Constraints Deep Dive

```sql
-- PRIMARY KEY: unique + not null + creates B-tree index
-- UNIQUE: allows one NULL (NULLs are not equal to each other)
-- CHECK: arbitrary boolean expression
-- FOREIGN KEY: referential integrity
-- NOT NULL: column-level constraint

CREATE TABLE orders (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id)
                    ON DELETE RESTRICT      -- Prevent deleting user with orders
                    ON UPDATE CASCADE,      -- Update FK if PK changes
    total_cents INTEGER NOT NULL CHECK (total_cents >= 0),
    status      TEXT NOT NULL
                    CHECK (status IN ('pending','paid','shipped','cancelled')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Deferrable constraints (checked at end of transaction, not per statement)
ALTER TABLE orders
    ADD CONSTRAINT orders_user_fk
    FOREIGN KEY (user_id) REFERENCES users(id)
    DEFERRABLE INITIALLY DEFERRED;
```

---

## Day 3 — CRUD, Filtering & Operators

### 3.1 INSERT

```sql
-- Basic insert
INSERT INTO users (email, username, password_hash)
VALUES ('alice@example.com', 'alice', 'bcrypt_hash_here');

-- Multi-row insert (much faster than individual inserts)
INSERT INTO users (email, username, password_hash)
VALUES
    ('bob@example.com',   'bob',   'hash2'),
    ('carol@example.com', 'carol', 'hash3'),
    ('dave@example.com',  'dave',  'hash4');

-- INSERT ... RETURNING (get back generated values)
INSERT INTO users (email, username, password_hash)
VALUES ('eve@example.com', 'eve', 'hash5')
RETURNING id, created_at;

-- INSERT ... ON CONFLICT (upsert)
INSERT INTO users (email, username, password_hash)
VALUES ('alice@example.com', 'alice2', 'newhash')
ON CONFLICT (email)
    DO UPDATE SET
        username     = EXCLUDED.username,
        password_hash = EXCLUDED.password_hash,
        updated_at   = NOW();

-- On conflict do nothing
INSERT INTO users (email, username, password_hash)
VALUES ('alice@example.com', 'alice', 'hash')
ON CONFLICT (email) DO NOTHING;

-- INSERT from SELECT (bulk insert from another table)
INSERT INTO users_archive
SELECT * FROM users WHERE deleted_at IS NOT NULL;
```

### 3.2 SELECT & Filtering

```sql
-- Basic select
SELECT id, email, created_at FROM users;

-- All columns (avoid in production code — brittle)
SELECT * FROM users;

-- Aliases
SELECT id AS user_id, email AS user_email FROM users;

-- WHERE clause
SELECT * FROM users WHERE is_active = TRUE;
SELECT * FROM users WHERE created_at > '2025-01-01';
SELECT * FROM users WHERE created_at BETWEEN '2025-01-01' AND '2025-12-31';

-- NULL handling
SELECT * FROM users WHERE deleted_at IS NULL;
SELECT * FROM users WHERE deleted_at IS NOT NULL;

-- IN operator
SELECT * FROM users WHERE role IN ('admin', 'editor');

-- LIKE / ILIKE (case-insensitive LIKE)
SELECT * FROM users WHERE email LIKE '%@example.com';
SELECT * FROM users WHERE username ILIKE 'ali%';

-- Regular expression
SELECT * FROM users WHERE email ~ '^admin';     -- case-sensitive
SELECT * FROM users WHERE email ~* '^admin';    -- case-insensitive
SELECT * FROM users WHERE email !~ 'test';      -- NOT matching

-- DISTINCT
SELECT DISTINCT role FROM users;
SELECT DISTINCT ON (email) * FROM users ORDER BY email, created_at DESC;
-- DISTINCT ON: keep first row per group (PostgreSQL extension)

-- ORDER BY
SELECT * FROM users ORDER BY created_at DESC;
SELECT * FROM users ORDER BY last_name ASC NULLS LAST;

-- LIMIT / OFFSET (pagination — beware of deep offsets)
SELECT * FROM users ORDER BY id LIMIT 20 OFFSET 40;
-- Better: keyset pagination
SELECT * FROM users WHERE id > 40 ORDER BY id LIMIT 20;
```

### 3.3 UPDATE

```sql
-- Basic update
UPDATE users SET is_active = FALSE WHERE id = 5;

-- Update multiple columns
UPDATE users
SET
    full_name  = 'Alice Smith',
    updated_at = NOW()
WHERE email = 'alice@example.com';

-- UPDATE with RETURNING
UPDATE users
SET is_active = FALSE
WHERE deleted_at < NOW() - INTERVAL '1 year'
RETURNING id, email;

-- UPDATE with subquery
UPDATE orders o
SET status = 'cancelled'
FROM users u
WHERE o.user_id = u.id
  AND u.is_active = FALSE
  AND o.status = 'pending';
```

### 3.4 DELETE

```sql
-- Delete specific rows
DELETE FROM users WHERE id = 5;

-- Delete with JOIN (FROM clause)
DELETE FROM orders
USING users
WHERE orders.user_id = users.id
  AND users.email = 'spam@example.com';

-- Delete with RETURNING
DELETE FROM users
WHERE created_at < NOW() - INTERVAL '5 years'
RETURNING *;

-- TRUNCATE (much faster than DELETE for whole table — not logged per row)
TRUNCATE TABLE temp_data;
TRUNCATE TABLE orders, order_items CASCADE; -- CASCADE truncates dependent tables
-- WARNING: TRUNCATE does not fire row-level triggers
```

### 3.5 Type Casting & Conversion

```sql
-- Cast operators
SELECT '42'::INTEGER;
SELECT '2025-01-01'::DATE;
SELECT 3.14::NUMERIC(5,2);

-- CAST function (ANSI SQL)
SELECT CAST('42' AS INTEGER);

-- Implicit vs explicit casting
SELECT '2025-01-01' + INTERVAL '1 day';   -- implicit date cast

-- Useful conversion functions
SELECT TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS');
SELECT TO_DATE('01/06/2025', 'DD/MM/YYYY');
SELECT TO_NUMBER('1,234.56', '9,999.99');
SELECT TO_TIMESTAMP('2025-06-01 12:00:00', 'YYYY-MM-DD HH24:MI:SS');
```

---

## Day 4 — Joins, Subqueries & Set Operations

### 4.1 JOIN Types

```sql
-- Setup for examples
CREATE TABLE departments (id SERIAL PRIMARY KEY, name TEXT);
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name TEXT,
    dept_id INTEGER REFERENCES departments(id),
    salary NUMERIC(10,2)
);

-- INNER JOIN — only matching rows
SELECT e.name, d.name AS dept
FROM employees e
INNER JOIN departments d ON e.dept_id = d.id;

-- LEFT JOIN (LEFT OUTER JOIN) — all left rows, NULLs for unmatched right
SELECT e.name, d.name AS dept
FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id;
-- Employees without a department get d.name = NULL

-- RIGHT JOIN — all right rows, NULLs for unmatched left (less common, flip JOIN instead)
SELECT e.name, d.name AS dept
FROM employees e
RIGHT JOIN departments d ON e.dept_id = d.id;

-- FULL OUTER JOIN — all rows from both sides
SELECT e.name, d.name AS dept
FROM employees e
FULL OUTER JOIN departments d ON e.dept_id = d.id;

-- CROSS JOIN — cartesian product
SELECT e.name, d.name
FROM employees e
CROSS JOIN departments d;
-- Result: every employee paired with every department

-- SELF JOIN — join table with itself
SELECT e1.name AS employee, e2.name AS manager
FROM employees e1
JOIN employees e2 ON e1.manager_id = e2.id;

-- NATURAL JOIN (avoid in production — fragile)
SELECT * FROM employees NATURAL JOIN departments;
-- Joins on columns with the same name automatically

-- LATERAL JOIN — execute subquery for each row (like a correlated subquery in FROM)
SELECT d.name, recent.name AS recent_hire
FROM departments d
LEFT JOIN LATERAL (
    SELECT name FROM employees
    WHERE dept_id = d.id
    ORDER BY created_at DESC
    LIMIT 1
) recent ON TRUE;
```

### 4.2 Subqueries

```sql
-- Scalar subquery (returns one value)
SELECT name,
       salary,
       (SELECT AVG(salary) FROM employees) AS company_avg,
       salary - (SELECT AVG(salary) FROM employees) AS diff
FROM employees;

-- Row subquery in WHERE
SELECT * FROM employees
WHERE dept_id = (SELECT id FROM departments WHERE name = 'Engineering');

-- IN subquery (list membership)
SELECT * FROM employees
WHERE dept_id IN (
    SELECT id FROM departments WHERE name IN ('Engineering', 'Product')
);

-- EXISTS (efficient — stops at first match)
SELECT * FROM departments d
WHERE EXISTS (
    SELECT 1 FROM employees e WHERE e.dept_id = d.id
);

-- NOT EXISTS
SELECT * FROM departments d
WHERE NOT EXISTS (
    SELECT 1 FROM employees e WHERE e.dept_id = d.id
);

-- Correlated subquery (references outer query — runs once per row)
SELECT name, salary
FROM employees e1
WHERE salary > (
    SELECT AVG(salary)
    FROM employees e2
    WHERE e2.dept_id = e1.dept_id  -- correlated!
);

-- Subquery in FROM (derived table / inline view)
SELECT dept_name, avg_salary
FROM (
    SELECT d.name AS dept_name, AVG(e.salary) AS avg_salary
    FROM departments d
    JOIN employees e ON e.dept_id = d.id
    GROUP BY d.name
) dept_stats
WHERE avg_salary > 80000;
```

### 4.3 Common Table Expressions (CTEs)

```sql
-- Basic CTE (named inline view, evaluated once)
WITH dept_stats AS (
    SELECT dept_id, AVG(salary) AS avg_salary, COUNT(*) AS headcount
    FROM employees
    GROUP BY dept_id
),
dept_info AS (
    SELECT d.id, d.name, s.avg_salary, s.headcount
    FROM departments d
    JOIN dept_stats s ON d.id = s.dept_id
)
SELECT * FROM dept_info WHERE avg_salary > 80000;

-- Recursive CTE — for hierarchical/graph data
WITH RECURSIVE org_chart AS (
    -- Base case: top-level managers (no manager)
    SELECT id, name, manager_id, 0 AS depth, name::TEXT AS path
    FROM employees
    WHERE manager_id IS NULL

    UNION ALL

    -- Recursive case: add reports
    SELECT e.id, e.name, e.manager_id, oc.depth + 1,
           oc.path || ' → ' || e.name
    FROM employees e
    JOIN org_chart oc ON e.manager_id = oc.id
    WHERE oc.depth < 10  -- safety limit
)
SELECT * FROM org_chart ORDER BY path;
```

### 4.4 Set Operations

```sql
-- UNION — combine results, remove duplicates
SELECT email FROM users
UNION
SELECT email FROM newsletter_subscribers;

-- UNION ALL — combine results, keep duplicates (faster!)
SELECT 'user' AS source, email FROM users
UNION ALL
SELECT 'subscriber', email FROM newsletter_subscribers;

-- INTERSECT — rows in both result sets
SELECT email FROM users
INTERSECT
SELECT email FROM newsletter_subscribers;

-- EXCEPT — rows in first but not second
SELECT email FROM users
EXCEPT
SELECT email FROM newsletter_subscribers;
-- Returns users who are NOT newsletter subscribers
```

---

## Day 5 — Aggregations, Window Functions & Advanced Grouping

### 5.1 Aggregate Functions

```sql
-- Basic aggregates
SELECT
    COUNT(*)                    AS total_rows,
    COUNT(DISTINCT dept_id)     AS unique_depts,
    COUNT(salary)               AS non_null_salaries,
    SUM(salary)                 AS total_payroll,
    AVG(salary)                 AS avg_salary,
    MIN(salary)                 AS min_salary,
    MAX(salary)                 AS max_salary,
    STDDEV(salary)              AS salary_stddev,
    VARIANCE(salary)            AS salary_variance
FROM employees;

-- Statistical aggregates
SELECT
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY salary) AS median_salary,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY salary) AS p95_salary,
    PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY salary) AS median_disc
FROM employees;

-- String aggregation
SELECT dept_id, STRING_AGG(name, ', ' ORDER BY name) AS employee_list
FROM employees
GROUP BY dept_id;

-- Array aggregation
SELECT dept_id, ARRAY_AGG(name ORDER BY salary DESC) AS employees_by_salary
FROM employees
GROUP BY dept_id;

-- GROUP BY
SELECT dept_id, role, COUNT(*), AVG(salary)
FROM employees
GROUP BY dept_id, role
ORDER BY dept_id, role;

-- HAVING (filter on aggregated values)
SELECT dept_id, COUNT(*) AS headcount, AVG(salary) AS avg_salary
FROM employees
GROUP BY dept_id
HAVING COUNT(*) >= 5
   AND AVG(salary) > 75000;

-- FILTER clause (conditional aggregation — very powerful!)
SELECT
    COUNT(*) FILTER (WHERE salary < 60000)  AS junior_count,
    COUNT(*) FILTER (WHERE salary < 100000) AS mid_count,
    COUNT(*) FILTER (WHERE salary >= 100000) AS senior_count,
    AVG(salary) FILTER (WHERE role = 'engineer') AS avg_engineer_salary
FROM employees;
```

### 5.2 Advanced Grouping

```sql
-- ROLLUP — subtotals and grand total
SELECT
    COALESCE(dept_id::TEXT, 'ALL') AS dept,
    COALESCE(role, 'ALL') AS role,
    COUNT(*),
    SUM(salary)
FROM employees
GROUP BY ROLLUP(dept_id, role);
-- Produces: (dept, role), (dept, ALL), (ALL, ALL)

-- CUBE — all combinations of groupings
SELECT dept_id, role, year, SUM(salary)
FROM employees
GROUP BY CUBE(dept_id, role, year);

-- GROUPING SETS — specify exact groupings
SELECT dept_id, role, SUM(salary)
FROM employees
GROUP BY GROUPING SETS (
    (dept_id, role),   -- by dept and role
    (dept_id),         -- by dept only
    (role),            -- by role only
    ()                 -- grand total
);
```

### 5.3 Window Functions — The Crown Jewel

Window functions perform calculations across related rows **without collapsing them** (unlike GROUP BY).

```sql
-- Syntax:
-- function() OVER (
--     [PARTITION BY column(s)]
--     [ORDER BY column(s)]
--     [frame_clause]
-- )

-- ROW_NUMBER — unique rank per partition
SELECT
    name, dept_id, salary,
    ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rank_in_dept
FROM employees;

-- RANK — rank with gaps for ties
SELECT name, salary,
    RANK() OVER (ORDER BY salary DESC) AS overall_rank
FROM employees;
-- Ties: 1, 1, 3 (skips 2)

-- DENSE_RANK — rank without gaps
SELECT name, salary,
    DENSE_RANK() OVER (ORDER BY salary DESC) AS dense_rank
FROM employees;
-- Ties: 1, 1, 2 (no gaps)

-- NTILE — divide into N buckets
SELECT name, salary,
    NTILE(4) OVER (ORDER BY salary) AS quartile
FROM employees;

-- LAG / LEAD — access adjacent rows
SELECT
    id, name, salary,
    LAG(salary, 1) OVER (ORDER BY id) AS prev_salary,
    LEAD(salary, 1) OVER (ORDER BY id) AS next_salary,
    salary - LAG(salary, 1) OVER (ORDER BY id) AS salary_change
FROM employees;

-- FIRST_VALUE / LAST_VALUE / NTH_VALUE
SELECT
    name, dept_id, salary,
    FIRST_VALUE(salary) OVER (PARTITION BY dept_id ORDER BY salary DESC) AS highest_in_dept,
    LAST_VALUE(salary) OVER (
        PARTITION BY dept_id
        ORDER BY salary DESC
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS lowest_in_dept
FROM employees;

-- Running totals / cumulative sums
SELECT
    created_at::DATE AS day,
    COUNT(*) AS daily_orders,
    SUM(COUNT(*)) OVER (ORDER BY created_at::DATE) AS cumulative_orders
FROM orders
GROUP BY created_at::DATE;

-- Moving average (7-day)
SELECT
    created_at::DATE AS day,
    SUM(total_cents) / 100.0 AS daily_revenue,
    AVG(SUM(total_cents)) OVER (
        ORDER BY created_at::DATE
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ) / 100.0 AS rolling_7day_avg
FROM orders
GROUP BY created_at::DATE
ORDER BY day;

-- Percent of total per group
SELECT
    name, dept_id, salary,
    ROUND(100.0 * salary / SUM(salary) OVER (PARTITION BY dept_id), 2) AS pct_of_dept_payroll
FROM employees;
```

---

## Day 6 — Indexes — Types, Internals & Strategy

### 6.1 Why Indexes?

Without an index, PostgreSQL does a **sequential scan** — reading every row. For large tables, this is O(n). Indexes allow O(log n) lookups. But indexes have costs:
- **Write overhead:** Every INSERT/UPDATE/DELETE must also update the index.
- **Storage:** Indexes take disk space.
- **Planner confusion:** Too many indexes can confuse the query planner.

Rule: Index columns used in WHERE, JOIN, ORDER BY, GROUP BY. Don't over-index.

### 6.2 B-Tree Index (Default)

```sql
-- Default index type — B-Tree (Balanced Tree)
CREATE INDEX idx_users_email ON users(email);
CREATE UNIQUE INDEX idx_users_email_unique ON users(email);

-- Multi-column index
-- Order matters! Index on (a, b) can satisfy WHERE a=? and WHERE a=? AND b=?
-- but NOT WHERE b=? alone.
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

-- Partial index — only index rows matching condition
-- Saves space, faster for common query patterns
CREATE INDEX idx_orders_pending ON orders(user_id)
WHERE status = 'pending';

-- This query uses the partial index:
SELECT * FROM orders WHERE user_id = 5 AND status = 'pending';

-- Index on expression
CREATE INDEX idx_users_lower_email ON users(LOWER(email));
-- Enables: WHERE LOWER(email) = 'alice@example.com'

-- Covering index (INCLUDE) — store additional columns in index leaf
-- Allows index-only scans without hitting heap
CREATE INDEX idx_orders_covering ON orders(user_id)
INCLUDE (status, total_cents, created_at);
```

### 6.3 Other Index Types

```sql
-- HASH index — O(1) equality lookups (no range queries, no sorting)
-- Since PG 10 WAL-logged and crash-safe
CREATE INDEX idx_users_uuid_hash ON users USING HASH (uuid_field);

-- GIN (Generalized Inverted Index) — for array, JSONB, full-text search
-- Stores index entries for each element/key
CREATE INDEX idx_products_tags_gin ON products USING GIN (tags);     -- array
CREATE INDEX idx_docs_body_gin ON documents USING GIN (to_tsvector('english', body));  -- FTS
CREATE INDEX idx_users_prefs_gin ON users USING GIN (preferences);   -- JSONB

-- Queries using GIN:
SELECT * FROM products WHERE tags @> ARRAY['postgresql'];  -- array contains
SELECT * FROM users WHERE preferences @> '{"theme": "dark"}';  -- JSONB contains

-- GiST (Generalized Search Tree) — for geometric types, full-text, ranges
CREATE INDEX idx_events_duration ON events USING GIST (duration);   -- range overlap
CREATE INDEX idx_locations_point ON locations USING GIST (geom);    -- geometry

-- SP-GiST — space-partitioned GiST, for non-balanced data (quadtrees, tries)
-- BRIN (Block Range Index) — for naturally ordered data (timestamps, sequential IDs)
-- Very small, but only works when physical ordering matches query
CREATE INDEX idx_orders_created_brin ON orders USING BRIN (created_at);

-- Bloom index — probabilistic, good for multi-column equality searches
CREATE EXTENSION bloom;
CREATE INDEX idx_multi_bloom ON table USING bloom (col1, col2, col3);
```

### 6.4 Index Internals

**B-Tree Structure:**
- Root → Branch nodes → Leaf nodes
- Leaf nodes are doubly-linked for efficient range scans
- Each leaf entry: (key value, heap tuple pointer TID)
- Height is typically 3–4 for even large tables

**Heap Tuple Pointer (TID):** `(block_number, item_offset)` — physical location of the row.

**Index-Only Scan:** If all needed columns are in the index (covering index), PostgreSQL never touches the heap. Requires `VACUUM` to have run (visibility map must indicate pages are all-visible).

### 6.5 Checking Index Usage

```sql
-- Check index usage statistics
SELECT
    indexrelname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE relname = 'orders'
ORDER BY idx_scan DESC;

-- Find unused indexes (candidates for removal)
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelname NOT LIKE 'pg_%'
ORDER BY schemaname, tablename;

-- Index sizes
SELECT
    indexrelname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC;
```

---

## Day 7 — Transactions, MVCC & Isolation Levels

### 7.1 Transactions Basics

```sql
-- Explicit transaction
BEGIN;
    UPDATE accounts SET balance = balance - 100 WHERE id = 1;
    UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;

-- Rollback on error
BEGIN;
    INSERT INTO orders (user_id, total_cents) VALUES (5, 9999);
    -- Something goes wrong:
ROLLBACK;

-- Savepoints (partial rollback)
BEGIN;
    INSERT INTO orders (user_id, total_cents) VALUES (5, 100);
    SAVEPOINT order_saved;

    INSERT INTO order_items (order_id, product_id) VALUES (currval('orders_id_seq'), 99);
    -- product 99 doesn't exist → error
    ROLLBACK TO SAVEPOINT order_saved;

    -- Try again with valid product
    INSERT INTO order_items (order_id, product_id) VALUES (currval('orders_id_seq'), 1);
COMMIT;
```

### 7.2 MVCC — Multi-Version Concurrency Control

MVCC is the core mechanism enabling PostgreSQL's non-blocking reads. **Readers never block writers; writers never block readers.**

**How it works:**

Every row (tuple) has hidden system columns:
- `xmin` — Transaction ID that **created** this row version
- `xmax` — Transaction ID that **deleted/updated** this row version (0 if current)
- `ctid` — Physical location (block, offset)

```sql
-- See hidden columns
SELECT xmin, xmax, ctid, * FROM users WHERE id = 1;
```

**When you UPDATE a row:**
1. The old row version is marked with `xmax = current_txn_id`
2. A **new row version** is inserted with `xmin = current_txn_id`
3. Both versions coexist until VACUUM cleans up the old one

**Snapshot Isolation:** At transaction start, PostgreSQL takes a snapshot of which transactions are committed. It only sees rows where `xmin` is committed in the snapshot and `xmax` is either 0 or not committed in the snapshot.

This means: Long-running transactions hold back VACUUM from cleaning old row versions → **table bloat**. Monitor `n_dead_tup` in `pg_stat_user_tables`.

### 7.3 Isolation Levels

PostgreSQL supports all four SQL standard isolation levels:

| Level | Dirty Read | Non-Repeatable Read | Phantom Read | Serialization Anomaly |
|-------|-----------|---------------------|-------------|----------------------|
| Read Uncommitted | ✗ (N/A) | Possible | Possible | Possible |
| **Read Committed** (default) | No | Possible | Possible | Possible |
| Repeatable Read | No | No | No* | Possible |
| Serializable | No | No | No | No |

*PostgreSQL's MVCC prevents phantom reads in Repeatable Read.

```sql
-- Set isolation level
BEGIN ISOLATION LEVEL READ COMMITTED;      -- default
BEGIN ISOLATION LEVEL REPEATABLE READ;
BEGIN ISOLATION LEVEL SERIALIZABLE;

-- Or:
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
```

**Read Committed (default):**
- Each statement gets a fresh snapshot
- Same transaction can see different data in different statements
- Simple, good for OLTP

**Repeatable Read:**
- Snapshot taken at first statement of transaction
- All reads see same data throughout transaction
- Good for reports, batch jobs

**Serializable:**
- Transactions appear to execute one at a time
- Uses Serializable Snapshot Isolation (SSI) — detects read-write conflicts
- May get `ERROR: could not serialize access due to read/write dependencies`
- Always retry on serialization failure!
- Use for financial transactions, inventory management

### 7.4 Locking

```sql
-- Row-level locks
SELECT * FROM orders WHERE id = 1 FOR UPDATE;           -- exclusive lock
SELECT * FROM orders WHERE id = 1 FOR SHARE;            -- shared lock
SELECT * FROM orders WHERE id = 1 FOR UPDATE NOWAIT;    -- fail immediately if locked
SELECT * FROM orders WHERE id = 1 FOR UPDATE SKIP LOCKED; -- skip locked rows (job queues!)

-- Advisory locks (application-level, not tied to rows)
SELECT pg_advisory_lock(12345);           -- session-level, blocks
SELECT pg_advisory_xact_lock(12345);      -- transaction-level, auto-released
SELECT pg_try_advisory_lock(12345);       -- non-blocking, returns bool

-- View current locks
SELECT pid, mode, granted, relation::regclass, locktype
FROM pg_locks l
JOIN pg_class c ON l.relation = c.oid
WHERE NOT granted;

-- Detect deadlocks (PostgreSQL detects and kills one automatically)
-- Check pg_log for "deadlock detected"
```

---

## Day 8 — Stored Procedures, Functions & Triggers

### 8.1 PL/pgSQL Functions

```sql
-- Basic function
CREATE OR REPLACE FUNCTION add_numbers(a INTEGER, b INTEGER)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN a + b;
END;
$$;

SELECT add_numbers(3, 7);  -- Returns 10

-- Function with SQL language (simpler for pure SQL)
CREATE OR REPLACE FUNCTION get_user_email(user_id BIGINT)
RETURNS TEXT
LANGUAGE sql
STABLE  -- doesn't modify DB, result depends on args and DB state
AS $$
    SELECT email FROM users WHERE id = user_id;
$$;

-- Function returning a table
CREATE OR REPLACE FUNCTION get_active_users(dept_id INTEGER)
RETURNS TABLE(id BIGINT, name TEXT, email TEXT)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
        SELECT u.id, u.full_name, u.email
        FROM users u
        WHERE u.dept_id = $1
          AND u.is_active = TRUE
        ORDER BY u.full_name;
END;
$$;

SELECT * FROM get_active_users(3);

-- Control flow
CREATE OR REPLACE FUNCTION classify_salary(salary NUMERIC)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE  -- result depends only on arguments (no DB access)
AS $$
DECLARE
    result TEXT;
BEGIN
    IF salary < 50000 THEN
        result := 'junior';
    ELSIF salary < 100000 THEN
        result := 'mid';
    ELSIF salary < 150000 THEN
        result := 'senior';
    ELSE
        result := 'staff';
    END IF;
    RETURN result;
END;
$$;

-- Loop and exception handling
CREATE OR REPLACE FUNCTION safe_divide(a NUMERIC, b NUMERIC)
RETURNS NUMERIC
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    IF b = 0 THEN
        RAISE EXCEPTION 'Division by zero: cannot divide % by 0', a
            USING ERRCODE = 'division_by_zero';
    END IF;
    RETURN a / b;
EXCEPTION
    WHEN division_by_zero THEN
        RAISE NOTICE 'Caught division by zero, returning NULL';
        RETURN NULL;
END;
$$;
```

### 8.2 Stored Procedures (PG 11+)

Unlike functions, procedures can commit/rollback transactions.

```sql
CREATE OR REPLACE PROCEDURE transfer_funds(
    from_account_id BIGINT,
    to_account_id BIGINT,
    amount_cents INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    from_balance INTEGER;
BEGIN
    -- Lock both accounts (order by ID to prevent deadlock)
    SELECT balance_cents INTO from_balance
    FROM accounts
    WHERE id = from_account_id
    FOR UPDATE;

    IF from_balance < amount_cents THEN
        RAISE EXCEPTION 'Insufficient funds: balance=%, needed=%',
            from_balance, amount_cents;
    END IF;

    UPDATE accounts SET balance_cents = balance_cents - amount_cents
    WHERE id = from_account_id;

    UPDATE accounts SET balance_cents = balance_cents + amount_cents
    WHERE id = to_account_id;

    INSERT INTO transfer_log (from_id, to_id, amount_cents, transferred_at)
    VALUES (from_account_id, to_account_id, amount_cents, NOW());

    COMMIT;  -- procedures can commit!
END;
$$;

CALL transfer_funds(1, 2, 5000);
```

### 8.3 Triggers

```sql
-- Trigger function (must return TRIGGER)
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;  -- must return NEW for BEFORE triggers on rows
END;
$$;

-- Create trigger
CREATE TRIGGER users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- Audit trigger
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    table_name  TEXT,
    operation   TEXT,
    old_data    JSONB,
    new_data    JSONB,
    changed_by  TEXT DEFAULT CURRENT_USER,
    changed_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION audit_trigger_fn()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER  -- runs as function owner, not caller
AS $$
BEGIN
    INSERT INTO audit_log (table_name, operation, old_data, new_data)
    VALUES (
        TG_TABLE_NAME,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN row_to_json(OLD)::JSONB END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN row_to_json(NEW)::JSONB END
    );
    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER orders_audit
    AFTER INSERT OR UPDATE OR DELETE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION audit_trigger_fn();

-- Statement-level trigger
CREATE OR REPLACE FUNCTION log_bulk_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE NOTICE 'Bulk delete on % at %', TG_TABLE_NAME, NOW();
    RETURN NULL;
END;
$$;

CREATE TRIGGER orders_bulk_delete_log
    AFTER DELETE ON orders
    FOR EACH STATEMENT
    EXECUTE FUNCTION log_bulk_delete();
```

---

## Day 9 — Query Planner & EXPLAIN ANALYZE

### 9.1 How the Query Planner Works

PostgreSQL's query planner (optimizer) chooses the cheapest execution plan:

1. **Parse** SQL → Parse tree
2. **Analyze/Rewrite** → Query tree (apply views, rules)
3. **Plan** → Consider all join orders, index vs seq scan, etc.
4. **Execute** → Run the chosen plan

**Statistics:** The planner uses statistics in `pg_statistic` (updated by ANALYZE) to estimate:
- Table row counts
- Column value distribution (histograms)
- NULL fraction
- Most common values (MCV list)

```sql
-- Manually update statistics
ANALYZE users;
ANALYZE;  -- all tables

-- View table statistics
SELECT attname, n_distinct, correlation
FROM pg_stats
WHERE tablename = 'orders'
ORDER BY attname;
```

### 9.2 EXPLAIN & EXPLAIN ANALYZE

```sql
-- EXPLAIN — shows plan without executing
EXPLAIN SELECT * FROM users WHERE email = 'alice@example.com';

-- EXPLAIN ANALYZE — executes and shows actual vs estimated
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT u.name, COUNT(o.id) AS order_count
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
WHERE u.created_at > '2025-01-01'
GROUP BY u.id, u.name
ORDER BY order_count DESC;
```

**Reading EXPLAIN Output:**

```
Gather  (cost=1000.00..2847.93 rows=100 width=40) (actual time=5.2..12.4 rows=95 loops=1)
  Workers Planned: 2
  Workers Launched: 2
  ->  Hash Join  (cost=100.00..1750.00 rows=50 width=40) (actual time=3.1..8.2 rows=32 loops=3)
        Hash Cond: (o.user_id = u.id)
        Buffers: shared hit=450 read=120
        ->  Seq Scan on orders o  (cost=0..1500 rows=50000 width=20) (actual time=0.1..4.5 rows=50000 loops=3)
        ->  Hash  (cost=80.00..80.00 rows=2000 width=24) (actual time=2.0..2.0 rows=2000 loops=1)
              Buckets: 4096  Batches: 1  Memory Usage: 110kB
              ->  Index Scan using idx_users_created on users u  (cost=0..80 rows=2000 width=24)
                    Index Cond: (created_at > '2025-01-01')
```

**Key concepts:**
- `cost=X..Y` — startup cost .. total cost (in arbitrary planner units)
- `rows=N` — estimated rows (before: planner estimate, after: actual)
- `width=N` — estimated average row width in bytes
- `actual time=X..Y` — actual startup ms .. total ms
- `loops=N` — how many times this node was executed
- `Buffers: shared hit=X read=Y` — cache hits vs disk reads

**Red Flags:**
- Estimated vs actual rows differ by 10x+ → stale statistics
- `Seq Scan` on large table with WHERE clause → missing index
- `Hash Join` spilling to disk (`Batches > 1`) → increase `work_mem`
- `Nested Loop` with large outer set → may need index on inner table

### 9.3 Planner Configuration Knobs

```sql
-- Disable specific plan types for testing (never in production!)
SET enable_seqscan = OFF;
SET enable_hashjoin = OFF;
SET enable_mergejoin = OFF;
SET enable_nestloop = OFF;
SET enable_indexscan = OFF;
SET enable_bitmapscan = OFF;

-- Cost parameters (adjust to reflect actual hardware)
SET random_page_cost = 1.1;    -- SSD: 1.1, HDD: 4.0 (default)
SET seq_page_cost = 1.0;
SET cpu_tuple_cost = 0.01;

-- Parallel query settings
SET max_parallel_workers_per_gather = 4;
SET parallel_setup_cost = 1000;
SET parallel_tuple_cost = 0.1;

-- Force planner to get fresh statistics
SET default_statistics_target = 200;  -- default 100; increase for skewed columns
ALTER TABLE orders ALTER COLUMN status SET STATISTICS 500;
```

---

## Day 10 — Partitioning & Table Inheritance

### 10.1 Why Partition?

For very large tables (100M+ rows), partitioning:
- **Partition pruning:** Queries only scan relevant partitions
- **Faster VACUUM/maintenance:** Operate per-partition
- **Faster bulk deletes:** DROP PARTITION instead of DELETE + VACUUM
- **Parallel scans:** Each partition can be processed in parallel

### 10.2 Declarative Partitioning (PG 10+)

```sql
-- RANGE partitioning (most common for time-series data)
CREATE TABLE orders (
    id          BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    total_cents INTEGER NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status      TEXT NOT NULL
) PARTITION BY RANGE (created_at);

-- Create partitions
CREATE TABLE orders_2024 PARTITION OF orders
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE orders_2025 PARTITION OF orders
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE orders_2026 PARTITION OF orders
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

-- Default partition (catches anything that doesn't fit)
CREATE TABLE orders_default PARTITION OF orders DEFAULT;

-- Indexes are created per-partition
CREATE INDEX ON orders_2025 (user_id);
CREATE INDEX ON orders_2025 (created_at);
-- Or create globally (PG 11+) — automatically applied to all partitions
CREATE INDEX ON orders (user_id);

-- LIST partitioning
CREATE TABLE products (
    id     BIGINT NOT NULL,
    name   TEXT NOT NULL,
    region TEXT NOT NULL
) PARTITION BY LIST (region);

CREATE TABLE products_us    PARTITION OF products FOR VALUES IN ('US', 'CA');
CREATE TABLE products_eu    PARTITION OF products FOR VALUES IN ('DE', 'FR', 'UK');
CREATE TABLE products_apac  PARTITION OF products FOR VALUES IN ('JP', 'AU', 'SG');

-- HASH partitioning (distribute evenly by hash of key)
CREATE TABLE user_events (
    id      BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event   JSONB
) PARTITION BY HASH (user_id);

CREATE TABLE user_events_0 PARTITION OF user_events
    FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE user_events_1 PARTITION OF user_events
    FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE user_events_2 PARTITION OF user_events
    FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE user_events_3 PARTITION OF user_events
    FOR VALUES WITH (MODULUS 4, REMAINDER 3);

-- Partition management
ALTER TABLE orders DETACH PARTITION orders_2023;     -- remove partition
ALTER TABLE orders ATTACH PARTITION orders_archive   -- add existing table
    FOR VALUES FROM ('2020-01-01') TO ('2023-01-01');
```

### 10.3 Automatic Partition Creation

Use `pg_partman` extension for automatic partition management:

```sql
-- pg_partman handles creating future partitions and dropping old ones
-- Install: CREATE EXTENSION pg_partman;
-- Configure retention, premake count, etc.
```

### 10.4 Partition Pruning Verification

```sql
EXPLAIN SELECT * FROM orders WHERE created_at BETWEEN '2025-06-01' AND '2025-06-30';
-- Should show: "Partitions: orders_2025"
-- NOT "orders_2024, orders_2025, orders_2026"

-- Partition pruning requires the partition key in WHERE clause
-- Enable runtime pruning (default ON in PG 12+)
SET enable_partition_pruning = ON;
```

---

## Day 11 — Replication — Streaming, Logical & HA

### 11.1 Write-Ahead Log (WAL)

WAL is the foundation of replication and crash recovery:
- **Every change** is written to WAL before the actual data page
- WAL is an append-only log of 16MB segments in `pg_wal/`
- On crash, PostgreSQL replays WAL to restore consistent state
- Replicas apply WAL received from primary

```sql
-- WAL configuration
wal_level = replica          -- or logical (for logical replication)
max_wal_senders = 10         -- max concurrent WAL sender processes
wal_keep_size = 1024         -- keep 1GB of WAL for slow replicas (MB)
archive_mode = on            -- enable WAL archiving
archive_command = 'cp %p /wal_archive/%f'
```

### 11.2 Streaming Replication (Physical)

Copies the entire database cluster byte-for-byte. Replica is an exact copy.

**Primary setup:**
```sql
-- postgresql.conf
wal_level = replica
max_wal_senders = 5
wal_keep_size = 512

-- pg_hba.conf (allow replication connections)
host  replication  replicator  192.168.1.0/24  scram-sha-256
```

**Create replication user:**
```sql
CREATE USER replicator REPLICATION LOGIN PASSWORD 'replpass';
```

**Replica setup:**
```bash
# Take base backup from primary
pg_basebackup -h primary_host -U replicator -D /var/lib/postgresql/15/main \
  --wal-method=stream --checkpoint=fast --progress

# Create standby.signal file
touch /var/lib/postgresql/15/main/standby.signal

# postgresql.conf on replica
primary_conninfo = 'host=primary_host port=5432 user=replicator password=replpass'
hot_standby = on  # Allow read queries on replica
```

**Replication monitoring:**
```sql
-- On primary:
SELECT pid, application_name, state, sent_lsn, write_lsn, flush_lsn, replay_lsn,
       write_lag, flush_lag, replay_lag, sync_state
FROM pg_stat_replication;

-- On replica:
SELECT now() - pg_last_xact_replay_timestamp() AS replication_delay;
SELECT pg_is_in_recovery();  -- TRUE on replica
```

### 11.3 Synchronous vs Asynchronous Replication

```sql
-- postgresql.conf on primary
-- Asynchronous (default) — primary doesn't wait for replica ACK
-- Synchronous — primary waits for at least one replica to confirm WAL written

synchronous_standby_names = 'replica1'           -- wait for replica1
synchronous_standby_names = 'ANY 1 (rep1, rep2)' -- wait for any 1 of 2
synchronous_commit = on           -- default: wait for local WAL flush
synchronous_commit = remote_write -- wait for replica to receive + write (not flush)
synchronous_commit = remote_apply -- wait for replica to apply (replay)
```

### 11.4 Logical Replication (PG 10+)

Replicates at the row level (DML: INSERT, UPDATE, DELETE). Supports:
- Partial replication (selected tables)
- Cross-version replication
- Cross-platform replication
- Bi-directional replication (with care)

```sql
-- Primary (publisher):
-- postgresql.conf: wal_level = logical
CREATE PUBLICATION my_pub
    FOR TABLE users, orders, products;
-- Or: FOR ALL TABLES

-- Replica (subscriber):
CREATE SUBSCRIPTION my_sub
    CONNECTION 'host=primary_host user=replicator password=replpass dbname=mydb'
    PUBLICATION my_pub;

-- Monitor:
SELECT * FROM pg_stat_subscription;    -- on subscriber
SELECT * FROM pg_publication_tables;   -- on publisher
```

### 11.5 High Availability with Patroni

For production HA, use Patroni (manages automatic failover):

```
Architecture:
  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
  │  Primary    │────▶│  Replica 1  │     │  Replica 2  │
  │  PG + WAL  │     │  (standby)  │     │  (standby)  │
  └─────────────┘     └─────────────┘     └─────────────┘
         │                   │                   │
         └─────────┬─────────┘───────────────────┘
                   ▼
              ┌─────────┐
              │  etcd   │ (consensus store: who is primary?)
              └─────────┘
                   ▲
              ┌─────────┐
              │ HAProxy │ (routes reads/writes to correct node)
              └─────────┘
```

---

## Day 12 — JSON/JSONB, Full-Text Search & Extensions

### 12.1 JSON vs JSONB

| Feature | JSON | JSONB |
|---------|------|-------|
| Storage | Text (exact copy) | Binary decomposed |
| Write speed | Faster | Slightly slower |
| Read speed | Slower | Faster |
| Indexing | No GIN | Yes (GIN, path) |
| Key order | Preserved | Not preserved |
| Duplicate keys | Preserved | Last wins |

**Always use JSONB** unless you need exact JSON preservation (rare).

```sql
-- JSONB operators
SELECT '{"name": "Alice", "age": 30}'::JSONB -> 'name';       -- Returns JSON: "Alice"
SELECT '{"name": "Alice", "age": 30}'::JSONB ->> 'name';      -- Returns text: Alice
SELECT '{"a": {"b": 42}}'::JSONB #> '{a,b}';                  -- Path: returns 42 as JSON
SELECT '{"a": {"b": 42}}'::JSONB #>> '{a,b}';                 -- Path: returns "42" as text

-- Array indexing
SELECT '[1,2,3]'::JSONB -> 0;           -- First element
SELECT '[1,2,3]'::JSONB -> -1;          -- Last element

-- Containment
SELECT '{"a":1, "b":2}'::JSONB @> '{"a":1}';   -- TRUE: left contains right
SELECT '{"a":1}'::JSONB <@ '{"a":1, "b":2}';   -- TRUE: left is contained by right

-- Key existence
SELECT '{"a":1}'::JSONB ? 'a';          -- TRUE
SELECT '{"a":1}'::JSONB ?| ARRAY['a','b'];  -- TRUE (any key)
SELECT '{"a":1,"b":2}'::JSONB ?& ARRAY['a','b'];  -- TRUE (all keys)

-- Modification
SELECT '{"a":1}'::JSONB || '{"b":2}'::JSONB;    -- Merge: {"a":1,"b":2}
SELECT '{"a":1,"b":2}'::JSONB - 'b';            -- Delete key: {"a":1}
SELECT '{"a":{"b":1}}'::JSONB #- '{a,b}';       -- Delete path

-- jsonb_set — update value at path
SELECT jsonb_set('{"a":{"b":1}}'::JSONB, '{a,b}', '99'::JSONB);
-- Result: {"a": {"b": 99}}

-- Expand JSONB to rows
SELECT * FROM jsonb_each('{"a":1, "b":2}');        -- key, value
SELECT * FROM jsonb_each_text('{"a":1, "b":2}');   -- key, value (text)
SELECT jsonb_object_keys('{"a":1, "b":2}');         -- just keys
SELECT jsonb_array_elements('[1,2,3]'::JSONB);      -- expand array

-- Aggregate to JSONB
SELECT jsonb_agg(row_to_json(u)) FROM users u LIMIT 5;
SELECT jsonb_object_agg(username, email) FROM users;
```

### 12.2 Full-Text Search (FTS)

```sql
-- tsvector: processed document (lexemes)
SELECT to_tsvector('english', 'The quick brown foxes jumped');
-- Result: 'brown':3 'fox':4 'jump':5 'quick':2

-- tsquery: search query
SELECT to_tsquery('english', 'fox & jump');         -- AND
SELECT to_tsquery('english', 'fox | cat');          -- OR
SELECT to_tsquery('english', '!fox');               -- NOT
SELECT to_tsquery('english', 'quick <-> brown');    -- FOLLOWED BY (adjacent)
SELECT plainto_tsquery('english', 'quick brown');   -- plain text → AND query
SELECT websearch_to_tsquery('english', '"quick brown" fox OR cat');  -- Google-style

-- Search
SELECT title, body
FROM articles
WHERE to_tsvector('english', title || ' ' || body) @@ to_tsquery('english', 'postgresql & performance');

-- Optimized approach: use a generated column + GIN index
ALTER TABLE articles ADD COLUMN search_vector TSVECTOR
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(body, '')), 'B')
    ) STORED;

CREATE INDEX idx_articles_fts ON articles USING GIN (search_vector);

-- Fast search using the index
SELECT title, ts_rank(search_vector, q) AS rank
FROM articles, to_tsquery('english', 'postgresql & performance') q
WHERE search_vector @@ q
ORDER BY rank DESC
LIMIT 10;

-- Highlight matching terms
SELECT ts_headline('english', body, to_tsquery('english', 'postgresql'),
    'MaxWords=50, MinWords=20, StartSel=<mark>, StopSel=</mark>')
FROM articles
WHERE search_vector @@ to_tsquery('english', 'postgresql');
```

### 12.3 Important Extensions

```sql
-- pg_stat_statements — track query performance (essential!)
CREATE EXTENSION pg_stat_statements;
-- Add to postgresql.conf: shared_preload_libraries = 'pg_stat_statements'

SELECT query, calls, mean_exec_time, total_exec_time, rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;

-- pgcrypto — cryptographic functions
CREATE EXTENSION pgcrypto;
SELECT gen_random_uuid();                    -- UUID v4
SELECT crypt('mypassword', gen_salt('bf'));  -- bcrypt hash
SELECT crypt('mypassword', stored_hash) = stored_hash;  -- verify

-- uuid-ossp (alternative UUID generation)
CREATE EXTENSION "uuid-ossp";
SELECT uuid_generate_v4();

-- pg_trgm — trigram similarity (fuzzy search, LIKE optimization)
CREATE EXTENSION pg_trgm;
SELECT similarity('postgresql', 'postgresq');   -- 0.8
SELECT * FROM users WHERE username % 'alice';   -- fuzzy match

CREATE INDEX idx_users_username_trgm ON users USING GIN (username gin_trgm_ops);
-- Now LIKE/ILIKE with leading wildcards use the index!
SELECT * FROM users WHERE username ILIKE '%alice%';

-- hstore — key-value pairs (use JSONB instead for new code)
CREATE EXTENSION hstore;

-- tablefunc — crosstab (pivot tables)
CREATE EXTENSION tablefunc;

-- PostGIS — geospatial (massive extension)
CREATE EXTENSION postgis;
SELECT ST_Distance(
    ST_MakePoint(-122.4194, 37.7749)::geography,  -- San Francisco
    ST_MakePoint(-73.9857, 40.7484)::geography     -- New York
) / 1000 AS km;

-- timescaledb — time-series optimization
-- pg_partman — automatic partition management
```

---

## Day 13 — Security, Roles, RLS & Auditing

### 13.1 Roles & Privileges

```sql
-- Create roles (roles can have login or not)
CREATE ROLE app_readonly;
CREATE ROLE app_readwrite;
CREATE ROLE app_admin;

-- Create users (roles with login)
CREATE USER appuser WITH PASSWORD 'secure_password';
CREATE USER reporting WITH PASSWORD 'report_pass';

-- Grant role membership
GRANT app_readonly TO reporting;
GRANT app_readwrite TO appuser;

-- Grant privileges on schema
GRANT USAGE ON SCHEMA app TO app_readonly, app_readwrite;

-- Grant table privileges
GRANT SELECT ON ALL TABLES IN SCHEMA app TO app_readonly;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA app TO app_readwrite;

-- Future tables (important! Otherwise new tables have no grants)
ALTER DEFAULT PRIVILEGES IN SCHEMA app
    GRANT SELECT ON TABLES TO app_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA app
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_readwrite;

-- Grant sequence usage
GRANT USAGE ON ALL SEQUENCES IN SCHEMA app TO app_readwrite;

-- Revoke public schema access (security best practice for PG 14 and earlier)
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE mydb FROM PUBLIC;
```

### 13.2 pg_hba.conf — Authentication

```
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   all             postgres                                peer
local   all             all                                     md5
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256
host    replication     replicator      192.168.1.0/24          scram-sha-256
host    all             all             0.0.0.0/0               reject
```

Authentication methods:
- `peer` — OS username must match PostgreSQL username (local only)
- `md5` — MD5-hashed password (legacy, avoid)
- `scram-sha-256` — Modern password hashing (preferred)
- `cert` — SSL client certificate
- `ldap` — LDAP server
- `reject` — Always reject

### 13.3 Row-Level Security (RLS)

RLS lets you filter rows based on the current user — perfect for multi-tenant systems.

```sql
-- Enable RLS on a table
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;  -- applies even to table owner

-- Create policies
-- SELECT policy: users can only see their own orders
CREATE POLICY orders_user_select ON orders
    FOR SELECT
    USING (user_id = current_setting('app.current_user_id')::BIGINT);

-- INSERT policy: users can only insert their own orders
CREATE POLICY orders_user_insert ON orders
    FOR INSERT
    WITH CHECK (user_id = current_setting('app.current_user_id')::BIGINT);

-- Admin bypass policy
CREATE POLICY orders_admin_all ON orders
    FOR ALL
    USING (current_setting('app.user_role') = 'admin');

-- In application:
SET LOCAL app.current_user_id = '42';
SET LOCAL app.user_role = 'viewer';
SELECT * FROM orders;  -- Only sees orders where user_id = 42

-- Multi-tenant example
CREATE TABLE tenants (id BIGINT PRIMARY KEY, name TEXT);
CREATE TABLE data (id BIGINT PRIMARY KEY, tenant_id BIGINT, payload JSONB);

ALTER TABLE data ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON data
    USING (tenant_id = current_setting('app.tenant_id')::BIGINT);

-- Leakproof functions (prevent timing attacks in policies)
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS BIGINT AS $$
    SELECT current_setting('app.tenant_id')::BIGINT;
$$ LANGUAGE sql STABLE LEAKPROOF SECURITY DEFINER;
```

### 13.4 SSL/TLS Configuration

```sql
-- postgresql.conf
ssl = on
ssl_cert_file = 'server.crt'
ssl_key_file  = 'server.key'
ssl_ca_file   = 'root.crt'
ssl_ciphers   = 'HIGH:MEDIUM:+3DES:!aNULL'
ssl_min_protocol_version = 'TLSv1.2'

-- Connection string with SSL required
postgresql://user:pass@host/db?sslmode=require
postgresql://user:pass@host/db?sslmode=verify-full&sslrootcert=root.crt
```

---

## Day 14 — Performance Tuning, VACUUM & Production Ops

### 14.1 Key postgresql.conf Tuning Parameters

```ini
# Memory
shared_buffers = 8GB              # 25% of RAM
effective_cache_size = 24GB       # 75% of RAM (planner hint, not allocation)
work_mem = 64MB                   # Per sort/hash op. Total = connections × work_mem × ops
maintenance_work_mem = 2GB        # VACUUM, CREATE INDEX, etc.
wal_buffers = 64MB                # 3% of shared_buffers, max 64MB

# WAL & Checkpoints
checkpoint_completion_target = 0.9  # Spread checkpoint writes over 90% of interval
max_wal_size = 4GB                  # Max WAL size before forced checkpoint
min_wal_size = 1GB
wal_compression = on               # Compress WAL (CPU vs I/O trade-off)

# Connections
max_connections = 200              # Use PgBouncer/Pgpool for pooling!
# With PgBouncer: max_connections = 50–100, pool_size = 20 per DB

# Query Planner
random_page_cost = 1.1            # SSD: 1.1 (HDD default: 4.0)
effective_io_concurrency = 200    # SSD: 200 (HDD: 2)
default_statistics_target = 200   # More histogram buckets (default 100)

# Parallelism
max_worker_processes = 16
max_parallel_workers = 8
max_parallel_workers_per_gather = 4
max_parallel_maintenance_workers = 4

# Logging
log_min_duration_statement = 1000   # Log queries > 1 second
log_checkpoints = on
log_connections = on
log_lock_waits = on
log_temp_files = 0                  # Log all temp files
```

### 14.2 VACUUM — Dead Tuple Cleanup

MVCC creates dead tuples on every UPDATE/DELETE. VACUUM reclaims this space.

```sql
-- Manual VACUUM
VACUUM users;                     -- reclaim dead tuples, update FSM/VM
VACUUM ANALYZE users;             -- + update statistics
VACUUM VERBOSE users;             -- show progress
VACUUM FULL users;                -- rewrite table (locks! use pg_repack instead)
VACUUM FREEZE users;              -- advance oldest XID, prevent XID wraparound

-- Monitor table bloat
SELECT
    relname,
    n_live_tup,
    n_dead_tup,
    ROUND(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS dead_pct,
    last_vacuum,
    last_autovacuum,
    last_analyze
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;

-- Autovacuum configuration (per-table override)
ALTER TABLE high_churn_table SET (
    autovacuum_vacuum_scale_factor = 0.01,   -- vacuum when 1% dead (default 20%)
    autovacuum_analyze_scale_factor = 0.005, -- analyze when 0.5% changed
    autovacuum_vacuum_cost_delay = 2         -- less throttling (default 20ms)
);

-- XID wraparound prevention (critical!)
-- PostgreSQL XID is 32-bit: ~2 billion transactions
-- When approaching limit, PostgreSQL will SHUT DOWN to prevent data loss
SELECT
    datname,
    age(datfrozenxid) AS xid_age,
    2000000000 - age(datfrozenxid) AS remaining_transactions
FROM pg_database
ORDER BY xid_age DESC;
-- Alert if remaining_transactions < 100,000,000!
```

### 14.3 Connection Pooling with PgBouncer

Direct connections to PostgreSQL are expensive (fork a process). Pool with PgBouncer:

```ini
# pgbouncer.ini
[databases]
mydb = host=127.0.0.1 port=5432 dbname=mydb

[pgbouncer]
listen_port = 6432
listen_addr = *
auth_type = scram-sha-256
auth_file = /etc/pgbouncer/userlist.txt
pool_mode = transaction    # transaction pooling (most efficient)
max_client_conn = 10000   # clients can connect to PgBouncer
default_pool_size = 20    # actual PG connections per db/user pair
min_pool_size = 5
reserve_pool_size = 5
```

**Pool modes:**
- `session` — connection held for entire session (= no real pooling)
- `transaction` — connection returned after each transaction ✓ (best for OLTP)
- `statement` — returned after each statement (most aggressive, breaks multi-statement)

### 14.4 Slow Query Identification

```sql
-- Top slow queries (requires pg_stat_statements)
SELECT
    ROUND(mean_exec_time::NUMERIC, 2) AS mean_ms,
    ROUND(max_exec_time::NUMERIC, 2) AS max_ms,
    calls,
    ROUND(total_exec_time::NUMERIC, 2) AS total_ms,
    ROUND((total_exec_time / SUM(total_exec_time) OVER () * 100)::NUMERIC, 2) AS pct,
    LEFT(query, 120) AS query_sample
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;

-- Currently running long queries
SELECT pid, now() - pg_stat_activity.query_start AS duration,
       query, state, wait_event_type, wait_event
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > INTERVAL '5 minutes'
  AND state != 'idle';

-- Kill long query
SELECT pg_cancel_backend(pid);    -- graceful cancel (SIGINT)
SELECT pg_terminate_backend(pid); -- force terminate (SIGTERM)

-- Lock monitoring
SELECT
    blocked.pid, blocked.query, blocking.pid AS blocking_pid, blocking.query AS blocking_query
FROM pg_stat_activity AS blocked
JOIN pg_stat_activity AS blocking
    ON blocking.pid = ANY(pg_blocking_pids(blocked.pid))
WHERE cardinality(pg_blocking_pids(blocked.pid)) > 0;
```

### 14.5 Table & Database Maintenance

```sql
-- Table sizes
SELECT
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(oid)) AS total_size,
    pg_size_pretty(pg_relation_size(oid)) AS table_size,
    pg_size_pretty(pg_indexes_size(oid)) AS index_size
FROM pg_class
WHERE relkind = 'r'
  AND relnamespace = 'public'::regnamespace
ORDER BY pg_total_relation_size(oid) DESC;

-- Reindex (rebuild bloated indexes)
REINDEX TABLE CONCURRENTLY orders;  -- non-blocking (PG 12+)
REINDEX INDEX CONCURRENTLY idx_orders_user;

-- Bloat-free table rewrite (pg_repack extension — better than VACUUM FULL)
-- pg_repack --table orders --jobs 2 mydb

-- Backup with pg_dump
pg_dump -Fc -Z9 -j4 mydb > mydb_$(date +%Y%m%d).dump  # custom format, compressed, parallel
pg_restore -Fc -j4 -d mydb_restore mydb_20250601.dump

-- Continuous archiving / PITR
-- archive_command = 'wal-g wal-push %p'  (using WAL-G for cloud backups)
```

### 14.6 Monitoring Checklist

Essential metrics to monitor in production:

```sql
-- Cache hit ratio (should be > 99%)
SELECT
    SUM(blks_hit)::FLOAT / (SUM(blks_hit) + SUM(blks_read)) AS cache_hit_ratio
FROM pg_stat_database;

-- Replication lag (on replica)
SELECT EXTRACT(EPOCH FROM (now() - pg_last_xact_replay_timestamp())) AS lag_seconds;

-- Autovacuum workers
SELECT count(*) FROM pg_stat_activity WHERE query LIKE 'autovacuum:%';

-- Table bloat estimate
SELECT
    relname,
    pg_size_pretty(pg_relation_size(oid)) AS current_size,
    ROUND(100 * n_dead_tup::NUMERIC / NULLIF(n_live_tup + n_dead_tup, 0), 1) AS bloat_pct
FROM pg_stat_user_tables
WHERE n_dead_tup > 10000
ORDER BY n_dead_tup DESC;
```

---

## 🗺️ Learning Path Summary

```
Week 1: Foundations
  Day 1  ──▶  Architecture, psql CLI, connection setup
  Day 2  ──▶  Data types, DDL, constraints, schemas
  Day 3  ──▶  CRUD operations, filtering, type casting
  Day 4  ──▶  Joins, subqueries, CTEs, set operations
  Day 5  ──▶  Aggregations, window functions, grouping sets

Week 2: Mastery
  Day 6  ──▶  Indexes: B-Tree, GIN, GiST, BRIN, strategies
  Day 7  ──▶  MVCC, transactions, isolation levels, locking
  Day 8  ──▶  PL/pgSQL functions, procedures, triggers
  Day 9  ──▶  EXPLAIN ANALYZE, query planner, optimization
  Day 10 ──▶  Table partitioning (range, list, hash)
  Day 11 ──▶  Streaming & logical replication, HA
  Day 12 ──▶  JSONB, full-text search, extensions
  Day 13 ──▶  Security, RLS, roles, SSL
  Day 14 ──▶  Performance tuning, VACUUM, production ops
```

---

## 📚 Next Steps & Resources

| Resource | URL | Focus |
|----------|-----|-------|
| Official Docs | https://www.postgresql.org/docs/ | Comprehensive reference |
| The Internals of PG | https://www.interdb.jp/pg/ | Deep internals |
| PostgreSQL Exercises | https://pgexercises.com/ | Hands-on SQL practice |
| Use The Index, Luke | https://use-the-index-luke.com/ | Index deep dive |
| pg_activity | GitHub | Real-time monitoring |
| Pagila DB | GitHub | Sample database for practice |

---

*Generated for SDE 3-level interview and production readiness. Version: PostgreSQL 15/16.*
