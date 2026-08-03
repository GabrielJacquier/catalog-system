package com.catalog.consolidation.domain.model;

public record ProductInsertionResult(Product product, boolean inserted, boolean productLinkedToSeller) {

    public ProductInsertionResult withProductLinkedToSeller(boolean productLinkedToSeller) {
        return new ProductInsertionResult(product, inserted, productLinkedToSeller);
    }
}
