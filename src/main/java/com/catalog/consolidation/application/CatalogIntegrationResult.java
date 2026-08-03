package com.catalog.consolidation.application;

public record CatalogIntegrationResult(
        int productsInserted,
        int sellerLinksCreated,
        int sellerLinksSkipped,
        int totalProcessed
) {
}
