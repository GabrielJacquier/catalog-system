package com.catalog.consolidation.domain.model;

public record SellerProduct(
        Seller seller,
        String sellerProductId,
        String sellerProductName,
        String sellerBrand,
        String sellerCategory
) {
}
