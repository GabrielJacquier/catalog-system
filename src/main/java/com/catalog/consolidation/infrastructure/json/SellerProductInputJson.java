package com.catalog.consolidation.infrastructure.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SellerProductInputJson {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("SellerName")
    private String sellerName;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Brand")
    private String brand;

    @JsonProperty("Category")
    private String category;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
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
}
