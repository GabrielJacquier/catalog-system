# Architecture

## Overview

The catalog consolidation system ingests seller product JSON feeds into a shared marketplace catalog. It avoids duplicate canonical products while preserving per-seller listing data and keeping the legacy production flow safe.

## Stage 1 — Prepare database

### Context

The `Product` table is already used in production. All changes are **additive** and backward-compatible.

### `Availability`

| Value | Meaning |
|-------|---------|
| `AVAILABLE` | Visible to the existing production flow |
| `PENDING` | Integrated from sellers, pending review/activation |

Existing seed products are backfilled as `AVAILABLE`. New products from Stage 2 are inserted as `PENDING`.

Production queries should filter:

```sql
WHERE Availability = 'AVAILABLE'
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

Migration idempotency is detected by the presence of the `Availability` column on `Product`. If it already exists, `SchemaMigration.run()` skips execution.

## Stage 2 — Consolidate catalog

### Layered architecture

```
domain/             → entities, business rules (ProductMatcher, SellerProductPreparationService,
                       ProductInsertionService) and the persistence contracts it depends on
                       (ProductRepository, SellerProductRepository)
infrastructure/      → implements domain repository interfaces (JDBC), JSON reader + DTO + factory,
                       SchemaMigration
application/         → composition root (Application, CLI), ImportCatalogService (orchestration
                       and counters), ImportCatalogResult
```

Dependency direction: `application` knows both `domain` and `infrastructure` and wires concrete
classes together directly (no interface for the use case itself). `infrastructure` knows `domain`
and implements its repository interfaces. `domain` knows only itself — it never imports
`application` or `infrastructure`. The only inversion of dependency kept is the repository
contracts (`ProductRepository`, `SellerProductRepository`), because `ProductInsertionService`
(a domain service) needs to persist without depending on a concrete JDBC implementation.

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

`ProductRepository.insertIfNotExistsAndFetch` (domain interface, implemented by `SqliteProductRepository`):

1. `INSERT ... ON CONFLICT(NormalizedProductName, NormalizedBrand) DO NOTHING`
2. `SELECT` by normalized keys
3. Return `ProductUpsertResult(product, inserted)`

First-wins: existing production rows are never overwritten.

### Import flow

Per seller input item, `ProductInsertionService.insert` (domain) orchestrates:

1. `SellerProductPreparationService.prepareCandidate` → `Product` candidate (`PENDING`)
2. `ProductRepository.insertIfNotExistsAndFetch`
3. `SellerProductPreparationService.prepareLink` → `SellerProductLink`
4. `SellerProductRepository.link` with seller snapshot
5. Returns `ProductInsertionResult(upsertResult, linked)`

`ImportCatalogService.execute` (application) loops over the inputs, calls `ProductInsertionService.insert`
for each, and only counts the outcome (`productsInserted`, `sellerLinksCreated`, `sellerLinksSkipped`) —
it has no business logic of its own.

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
- Activation workflow for `PENDING` products
- Additional product identifiers (SKU, EAN) for deduplication
