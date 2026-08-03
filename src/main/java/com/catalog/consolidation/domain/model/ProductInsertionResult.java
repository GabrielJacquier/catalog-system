package com.catalog.consolidation.domain.model;

public record ProductInsertionResult(ProductUpsertResult upsertResult, boolean linked) {
}
