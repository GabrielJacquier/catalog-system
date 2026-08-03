package com.catalog.consolidation.domain.model;

public class Product {

    private Long id;
    private String name;
    private String brand;
    private String category;
    private String normalizedProductName;
    private String normalizedBrand;
    private SellerStatus sellerStatus;

    public Product() {
    }

    public Product(String name, String brand, String category,
                   String normalizedProductName, String normalizedBrand,
                   SellerStatus sellerStatus) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.normalizedProductName = normalizedProductName;
        this.normalizedBrand = normalizedBrand;
        this.sellerStatus = sellerStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNormalizedProductName() {
        return normalizedProductName;
    }

    public void setNormalizedProductName(String normalizedProductName) {
        this.normalizedProductName = normalizedProductName;
    }

    public String getNormalizedBrand() {
        return normalizedBrand;
    }

    public void setNormalizedBrand(String normalizedBrand) {
        this.normalizedBrand = normalizedBrand;
    }

    public SellerStatus getSellerStatus() {
        return sellerStatus;
    }

    public void setSellerStatus(SellerStatus sellerStatus) {
        this.sellerStatus = sellerStatus;
    }
}
