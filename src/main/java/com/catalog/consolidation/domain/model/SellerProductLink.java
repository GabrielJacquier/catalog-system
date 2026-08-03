package com.catalog.consolidation.domain.model;

public class SellerProductLink {

    private final String sellerName;
    private final String sellerProductId;
    private final String sellerProductName;
    private final String sellerBrand;
    private final String sellerCategory;

    public SellerProductLink(String sellerName, String sellerProductId,
                             String sellerProductName, String sellerBrand,
                             String sellerCategory) {
        this.sellerName = sellerName;
        this.sellerProductId = sellerProductId;
        this.sellerProductName = sellerProductName;
        this.sellerBrand = sellerBrand;
        this.sellerCategory = sellerCategory;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getSellerProductId() {
        return sellerProductId;
    }

    public String getSellerProductName() {
        return sellerProductName;
    }

    public String getSellerBrand() {
        return sellerBrand;
    }

    public String getSellerCategory() {
        return sellerCategory;
    }
}
