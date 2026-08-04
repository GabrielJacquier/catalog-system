package com.catalog.consolidation.domain.model;

public record ProductLinkedToSeller(
        Seller seller,
        Product product,
        String sellerProductId,
        String sellerProductName,
        String sellerBrand,
        String sellerCategory
) {
}
