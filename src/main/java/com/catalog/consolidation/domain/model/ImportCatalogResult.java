package com.catalog.consolidation.domain.model;

public record ImportCatalogResult(
        int productsInserted,
        int sellerLinksCreated,
        int sellerLinksSkipped,
        int totalProcessed
) {
}
