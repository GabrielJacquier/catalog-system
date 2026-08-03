package com.catalog.consolidation.application;

public record ImportCatalogResult(
        int productsInserted,
        int sellerLinksCreated,
        int sellerLinksSkipped,
        int totalProcessed
) {
}
