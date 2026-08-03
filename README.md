# Catalog Consolidation System

Marketplace catalog consolidation: ingest seller product feeds, deduplicate by normalized name and brand, and link sellers to canonical products without impacting the existing production catalog.

> **For reviewers / interviewers:** this README is a step-by-step guide to build, test, run, and inspect results end-to-end.

## Two-stage flow

1. **Stage 1 — Prepare database**: additive schema migration on the production `Product` table (`Availability`, normalized columns) and recreation of `SellerProduct`.
2. **Stage 2 — Consolidate catalog**: read `seller-products.json`, upsert products, and create seller links with original seller snapshots.

New products are inserted as `PENDING`. Existing production products remain `AVAILABLE`.

## Prerequisites

- **Option A (recommended):** Docker with Compose
- **Option B:** Java 17+ and Maven 3.9+

## Project layout

```
catalog-system/
├── samples/catalog.db            # seed SQLite database (production catalog)
├── samples/seller-products.json  # seller product feed
├── src/                          # Java source (layered: domain / infrastructure / application)
├── docs/architecture.md          # design decisions
├── Dockerfile
└── docker-compose.yml
```

`samples/catalog.db` and `samples/seller-products.json` are the fixed inputs (read-only, never
modified). Every run writes/updates `catalog-updated.db` at the project root.

---

## How to run (Docker — recommended)

### Step 1 — Build and run consolidation

From the project root:

```bash
docker compose up --build
```

This single command:

1. Builds the application image (Maven + Java 17)
2. Mounts `samples/catalog.db` and `samples/seller-products.json` read-only
3. Mounts the project root read-write so `catalog-updated.db` appears on your host
4. Runs **Stage 1** (schema migration) then **Stage 2** (catalog import)
5. Exits when finished (batch job, not a long-running server)

`catalog-updated.db` is created from the seed on the first run, and reused (accumulating changes) on
every subsequent run — the seed itself is never touched.

### Step 2 — Read the log output

You should see output similar to:

```
Using working database: /output/catalog-updated.db
Stage 1: Preparing database...
Stage 1 completed.
Stage 2: Importing catalog from /input/seller-products.json...
Stage 2 completed.
Summary:
  Total processed: 269
  Products inserted: 6
  Seller links created: 268
  Seller links skipped: 1
```

| Log field | Meaning |
|-----------|---------|
| `Total processed` | Number of entries read from `seller-products.json` |
| `Products inserted` | New canonical products created as `PENDING` (no normalized match existed) |
| `Seller links created` | New rows inserted into `SellerProduct` |
| `Seller links skipped` | Duplicate seller + `SellerProductId` already linked (`INSERT OR IGNORE`) |

**First run vs re-run:**

- **First run** (no `catalog-updated.db` yet): most seller links are created; `Products inserted`
  reflects genuinely new catalog items.
- **Second run** (same `catalog-updated.db`, same input): `Products inserted` → `0`,
  `Seller links created` → `0`, `Seller links skipped` → equals `Total processed` (full idempotency).

### Step 3 — Inspect the database

After the container exits, query `catalog-updated.db` at the project root:

```bash
docker run --rm -v "%cd%:/data" nouchka/sqlite3 /data/catalog-updated.db "SELECT Availability, COUNT(*) FROM Product GROUP BY Availability;"
```

On Linux/macOS, replace `%cd%` with `$(pwd)`.

**Expected result (first run on seed DB):**

```
AVAILABLE|975
PENDING|6
```

More validation queries are listed in [Validation queries](#validation-queries) below.

### Step 4 (optional) — Re-run from a clean seed

`catalog-updated.db` accumulates changes across runs since it's reused each time. To start over from
the untouched seed, just delete it before re-running:

```bash
rm catalog-updated.db   # PowerShell: Remove-Item catalog-updated.db
docker compose up --build
```

---

## How to run (local — Java + Maven)

```bash
mvn package
java -jar target/catalog-consolidation-1.0.0-SNAPSHOT.jar \
  --db samples/catalog.db \
  --input samples/seller-products.json \
  --output catalog-updated.db
```

CLI flags (all optional, same defaults as Docker):

| Flag | Default |
|------|---------|
| `--db` | `samples/catalog.db` |
| `--input` | `samples/seller-products.json` |
| `--output` | `catalog-updated.db` |

---

## Running tests with Docker

All tests (unit + integration) run inside Docker — no local Java 17 or Maven required.

### Run the full test suite

```bash
docker build --target test -t catalog-consolidation-test .
```

This builds the project and executes `mvn test`. A successful run ends with exit code `0`, but Maven
runs in quiet mode (`-q`), so you mostly only see output on failure.

### Run tests and see full live output

If you want to see each test class and its result printed as it runs (without waiting for the full
multi-stage image build), run Maven directly inside a throwaway container, mounting the project:

```bash
docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -B test
```

On Windows PowerShell, `${PWD}` resolves automatically. On Linux/macOS, use `$(pwd)` instead.

### What is covered

| Test class | Type | What it verifies |
|------------|------|------------------|
| `ProductNormalizationServiceTest` | Unit | Normalization rules (whitespace, accents, quotes, brand null) |
| `ProductFactoryTest` | Unit | Maps a JDBC `ResultSet` row into a `Product` domain model |
| `SellerProductFactoryTest` | Unit | Maps the JSON DTO into the `SellerProduct` domain model |
| `SchemaMigrationIT` | Integration | Stage 1 migration, backfill, idempotent re-run |
| `CatalogIntegrationAppIT` | Integration | Upsert, inactive new products, multi-seller links, idempotency |

### Run tests locally (if Maven is installed)

```bash
mvn test
```

---

## Validation queries

Run these against `catalog-updated.db` (at the project root) after consolidation.

### Product status breakdown

```sql
SELECT Availability, COUNT(*) AS total
FROM Product
GROUP BY Availability;
```

### Total seller links

```sql
SELECT COUNT(*) AS seller_product_links FROM SellerProduct;
```

### Seller snapshot preserved (canonical vs seller-original data)

Shows products where the seller sent a different name or category than the canonical `Product` row:

```sql
SELECT p.Name, sp.SellerProductName, sp.SellerCategory, sp.SellerName
FROM Product p
JOIN SellerProduct sp ON sp.ProductId = p.Id
WHERE p.Name != sp.SellerProductName OR p.Category != sp.SellerCategory
LIMIT 10;
```

### Products offered by multiple sellers

```sql
SELECT p.Name, p.Availability, COUNT(sp.Id) AS sellers
FROM Product p
JOIN SellerProduct sp ON sp.ProductId = p.Id
GROUP BY p.Id
HAVING sellers > 1;
```

### New inactive products from integration

```sql
SELECT Name, Brand, Category, Availability
FROM Product
WHERE Availability = 'PENDING';
```

### One-liner via Docker (copy-paste friendly)

**Windows (PowerShell):**

```powershell
docker run --rm -v "${PWD}:/data" nouchka/sqlite3 /data/catalog-updated.db "SELECT Availability, COUNT(*) FROM Product GROUP BY Availability; SELECT COUNT(*) FROM SellerProduct;"
```

**Linux / macOS:**

```bash
docker run --rm -v "$(pwd):/data" nouchka/sqlite3 /data/catalog-updated.db "SELECT Availability, COUNT(*) FROM Product GROUP BY Availability; SELECT COUNT(*) FROM SellerProduct;"
```

---

## Architecture

See [docs/architecture.md](docs/architecture.md) for design decisions, hexagonal layout, migration details, and JDBC security conventions.

## Key design decisions (summary)

| Topic | Decision |
|-------|----------|
| Duplicate detection | `NormalizedProductName` + `NormalizedBrand` (category excluded) |
| Production safety | Existing products → `AVAILABLE`; new imports → `PENDING` |
| Upsert | `INSERT ON CONFLICT DO NOTHING` + `SELECT` via `insertIfNotExistsAndFetch` |
| Seller data | Original name/brand/category stored in `SellerProduct` snapshot columns |
| SQL security | All runtime queries use `PreparedStatement` (no string concatenation) |
