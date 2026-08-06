package com.catalog.consolidation.domain.model;

public class Product {

    private Long id;
    private String name;
    private String brand;
    private String category;
    private String normalizedProductName;
    private String normalizedBrand;
    private String normalizedCategory;
    private Availability availability;

    public Product() {
    }

    public Product(String name, String brand, String category,
                   String normalizedProductName, String normalizedBrand,
                   String normalizedCategory, Availability availability) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.normalizedProductName = normalizedProductName;
        this.normalizedBrand = normalizedBrand;
        this.normalizedCategory = normalizedCategory;
        this.availability = availability;
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

    public String getNormalizedCategory() {
        return normalizedCategory;
    }

    public void setNormalizedCategory(String normalizedCategory) {
        this.normalizedCategory = normalizedCategory;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }
}
