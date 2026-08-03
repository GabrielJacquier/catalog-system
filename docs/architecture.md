# Architecture

## Overview

The catalog consolidation system ingests seller product JSON feeds into a shared marketplace catalog. It avoids duplicate canonical products while preserving per-seller listing data and keeping the legacy production flow safe.

## Stage 1 — Prepare database

### Context

The `Product` table is already used in production. All changes are **additive** and backward-compatible.

### `SellerStatus`

| Value | Meaning |
|-------|---------|
| `ACTIVE_TO_SELLER` | Visible to the existing production flow |
| `INACTIVE_TO_SELLER` | Integrated from sellers, pending review/activation |

Existing seed products are backfilled as `ACTIVE_TO_SELLER`. New products from Stage 2 are inserted as `INACTIVE_TO_SELLER`.

Production queries should filter:

```sql
WHERE SellerStatus = 'ACTIVE_TO_SELLER'
```

### Normalized columns

Duplicate detection uses two columns on `Product`:

- `NormalizedProductName`
- `NormalizedBrand`

A unique index on `(NormalizedProductName, NormalizedBrand)` supports `INSERT ... ON CONFLICT DO NOTHING`.

### `SellerProduct` schema

`SellerProduct` is recreated with seller snapshot columns:

- `SellerProductName`, `SellerBrand`, `SellerCategory` — original seller values
- `SellerProductId` — seller UUID from the feed (`TEXT`)
- `UNIQUE(SellerName, SellerProductId)` — idempotent reprocessing

### No dedicated `Seller` table

`SellerName` remains a text field on `SellerProduct`. A normalized `Seller` entity is deferred as future work.

### Migration idempotency

`schema_version` tracks applied migrations. `SchemaMigration.run()` applies version `1` only once.

## Stage 2 — Consolidate catalog

### Hexagonal architecture

```
bootstrap/          → composition root, CLI
domain/             → entities, ProductMatcher, ports
application/        → ImportCatalogService, DTOs, mappers
infrastructure/     → JDBC, JSON reader, SchemaMigration
```

### Duplicate definition

Same product = same `(NormalizedProductName, NormalizedBrand)`.

`Category` is stored on `Product` but **not** used for matching.

### Normalization rules (`ProductMatcher`)

| Rule | Example |
|------|---------|
| Trim and collapse whitespace | `"iPhone 15  Pro"` → `"iphone 15 pro"` |
| Lowercase | case-insensitive |
| Remove accents (NFD) | `"Câmera"` → `"camera"` |
| Normalize quotes | `''` and `"` → `'` |
| Null brand → empty string | `null` → `""` |

Semantic synonyms (e.g. `Router` vs `Roteador`) are **not** unified.

### Upsert pattern

`ProductRepository.insertIfNotExistsAndFetch`:

1. `INSERT ... ON CONFLICT(NormalizedProductName, NormalizedBrand) DO NOTHING`
2. `SELECT` by normalized keys
3. Return `ProductUpsertResult(product, inserted)`

First-wins: existing production rows are never overwritten.

### Import flow

`ImportCatalogService.execute`:

1. Map input → `Product` candidate (`INACTIVE_TO_SELLER`)
2. `insertIfNotExistsAndFetch`
3. `SellerProductRepository.link` with seller snapshot

### Seller conflicts

- Same seller + same `SellerProductId` → skipped (`INSERT OR IGNORE`)
- Same seller + different product for same ID → logged as inconsistent (unlikely in dataset)

## Infrastructure conventions

### JDBC security

**All runtime SQL uses `PreparedStatement` with bound parameters.** No string concatenation of user or seller input into queries.

DDL in migrations may be static strings. Data backfill uses prepared statements.

## Future improvements

- Dedicated `Seller` table with FK from `SellerProduct`
- Synonym dictionary for cross-language product names
- Activation workflow for `INACTIVE_TO_SELLER` products
- Additional product identifiers (SKU, EAN) for deduplication
