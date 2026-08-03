package com.catalog.consolidation.domain.model;

public record SellerProduct(
        String sellerName,
        String sellerProductId,
        String sellerProductName,
        String sellerBrand,
        String sellerCategory
) {
}
