package com.catalog.consolidation.domain.model;

public record SellerProductInput(
        String id,
        String sellerName,
        String name,
        String brand,
        String category
) {
}
