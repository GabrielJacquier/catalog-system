package com.catalog.consolidation.domain.model;

public record ProductInsertionResult(
        Product product,
        boolean inserted,
        boolean productLinkedToSeller,
        SellerProduct failedSellerProduct,
        String errorMessage
) {

    public ProductInsertionResult(Product product, boolean inserted, boolean productLinkedToSeller) {
        this(product, inserted, productLinkedToSeller, null, null);
    }

    public static ProductInsertionResult failure(SellerProduct source, String errorMessage) {
        return new ProductInsertionResult(null, false, false, source, errorMessage);
    }

    public boolean failed() {
        return failedSellerProduct != null;
    }

    public ProductInsertionResult withProductLinkedToSeller(boolean productLinkedToSeller) {
        return new ProductInsertionResult(product, inserted, productLinkedToSeller, failedSellerProduct, errorMessage);
    }
}
