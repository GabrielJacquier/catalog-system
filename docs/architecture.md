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

### `Seller` schema

`Seller` stores the canonical seller identity:

- `Name` — original seller name from the feed (first-wins on insert)
- `NormalizedName` — uppercase key used for matching (`UNIQUE`)

### `SellerProduct` schema

`SellerProduct` is recreated with a foreign key to `Seller` and seller snapshot columns:

- `SellerId` — FK to `Seller(Id)`
- `SellerProductName`, `SellerBrand`, `SellerCategory` — original seller values
- `SellerProductId` — seller UUID from the feed (`TEXT`)
- `UNIQUE(SellerId, SellerProductId)` — idempotent reprocessing

### Migration idempotency

Migration idempotency is detected by the presence of the `Availability` column on `Product`. If it already exists, `SchemaMigration.run()` skips execution.

## Stage 2 — Consolidate catalog

### Layered architecture

```
domain/             → entities, business rules (ProductNormalizationService, ProductInsertionService)
                       and the persistence contracts it depends on
                       (ProductRepository, SellerRepository, SellerProductRepository)
infrastructure/      → implements domain repository interfaces (JDBC), JSON reader + DTO + factory,
                       SchemaMigration
application/         → Application (composition root, CLI) creates all dependencies and hands them
                       to CatalogIntegrationApp, which runs the migration, reads the catalog,
                       processes products (counters, continue-on-error), writes failed items JSON,
                       and prints the summary; CatalogIntegrationResult
```

Dependency direction: `application` knows both `domain` and `infrastructure` and wires concrete
classes together directly (no interface for the use case itself). `infrastructure` knows `domain`
and implements its repository interfaces. `domain` knows only itself — it never imports
`application` or `infrastructure`. The only inversion of dependency kept is the repository
contracts (`ProductRepository`, `SellerRepository`, `SellerProductRepository`), because `ProductInsertionService`
(a domain service) needs to persist without depending on a concrete JDBC implementation.

### Duplicate definition

Same product = same `(NormalizedProductName, NormalizedBrand)`.

`Category` is stored on `Product` but **not** used for matching.

### Normalization rules (`ProductNormalizationService`)

| Rule | Example |
|------|---------|
| Trim and collapse whitespace | `"iPhone 15  Pro"` → `"iphone 15 pro"` |
| Lowercase | case-insensitive |
| Remove accents (NFD) | `"Câmera"` → `"camera"` |
| Normalize quotes | `''` and `"` → `'` |
| Null brand → empty string | `null` → `""` |
| Seller name → trim + uppercase | `"MegaStore"` → `"MEGASTORE"` |

Semantic synonyms (e.g. `Router` vs `Roteador`) are **not** unified.

### Upsert pattern

`ProductRepository.insertIfNotExistsAndFetch` (domain interface, implemented by `SqliteProductRepository`):

1. `INSERT ... ON CONFLICT(NormalizedProductName, NormalizedBrand) DO NOTHING`
2. `SELECT` by normalized keys
3. Return `ProductInsertionResult(product, inserted, productLinkedToSeller = false)`

`SellerRepository.insertIfNotExistsAndFetch` (implemented by `SqliteSellerRepository`):

1. `INSERT ... ON CONFLICT(NormalizedName) DO NOTHING`
2. `SELECT` by `NormalizedName`

First-wins: existing production rows are never overwritten.

### Import flow

Per `SellerProduct` item, `ProductInsertionService.insert` (domain) orchestrates:

1. `buildSeller(sellerProduct)` → normalize seller name, then `SellerRepository.insertIfNotExistsAndFetch`
2. Rebuild `SellerProduct` with the persisted `Seller` (id filled)
3. `buildProduct(sellerProduct)` → `Product` candidate (`PENDING`), using `ProductNormalizationService`
4. `ProductRepository.insertIfNotExistsAndFetch` → `ProductInsertionResult` (`productLinkedToSeller = false`)
5. `SellerProductRepository.link` with the `SellerProduct` seller snapshot (`SellerId`)
6. Returns `result.withProductLinkedToSeller(linked)`

`CatalogIntegrationApp.startApp` (application) orchestrates the whole flow: runs the schema migration,
reads the catalog via `JsonCatalogReader`, delegates to `processProducts` (which loops over the inputs,
calls `ProductInsertionService.insert` for each, and counts the outcome — `productsInserted`,
`sellerLinksCreated`, `sellerLinksSkipped`, `itemsFailed`), writes failed items to a JSON file when any
exist (`--errors-output`, default `failed-seller-products.json`), and finally prints the summary. It has no
business logic of its own — `Application` (the CLI entrypoint) only builds every dependency and hands them to it.

Per-item failures: `ProductInsertionService.insert` catches unexpected exceptions, returns
`ProductInsertionResult.failure(source, errorMessage)` without aborting the batch, and the application
persists those failures as input-shaped JSON plus an `ErrorMessage` field.

### Seller conflicts

- Same seller + same `SellerProductId` → skipped (`INSERT OR IGNORE`)
- Same seller + different product for same ID → logged as inconsistent (unlikely in dataset)

## Infrastructure conventions

### JDBC security

**All runtime SQL uses `PreparedStatement` with bound parameters.** No string concatenation of user or seller input into queries.

DDL in migrations may be static strings. Data backfill uses prepared statements.

## Future improvements

- Synonym dictionary for cross-language product names
- Activation workflow for `PENDING` products
- Additional product identifiers (SKU, EAN) for deduplication
