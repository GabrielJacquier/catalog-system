package com.catalog.consolidation.application;

import com.catalog.consolidation.domain.model.ProductInsertionResult;

import java.util.List;

public record CatalogIntegrationResult(
        int productsInserted,
        int sellerLinksCreated,
        int sellerLinksSkipped,
        int totalProcessed,
        int itemsFailed,
        List<ProductInsertionResult> failures
) {
}
