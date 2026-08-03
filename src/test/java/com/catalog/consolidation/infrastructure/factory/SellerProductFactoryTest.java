package com.catalog.consolidation.infrastructure.factory;

import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.infrastructure.json.SellerProductInputJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SellerProductFactoryTest {

    private final SellerProductFactory factory = new SellerProductFactory();

    @Test
    void shouldCreateDomainModelFromJsonDto() {
        SellerProductInputJson json = new SellerProductInputJson();
        json.setId("seller-1");
        json.setSellerName("MegaStore");
        json.setName("Smartphone Galaxy S23");
        json.setBrand("Samsung");
        json.setCategory("Electronics");

        SellerProduct result = factory.create(json);

        assertThat(result.sellerProductId()).isEqualTo("seller-1");
        assertThat(result.sellerName()).isEqualTo("MegaStore");
        assertThat(result.sellerProductName()).isEqualTo("Smartphone Galaxy S23");
        assertThat(result.sellerBrand()).isEqualTo("Samsung");
        assertThat(result.sellerCategory()).isEqualTo("Electronics");
    }

    @Test
    void shouldHandleNullBrandAndCategory() {
        SellerProductInputJson json = new SellerProductInputJson();
        json.setId("seller-2");
        json.setSellerName("StoreB");
        json.setName("Generic Product");
        json.setBrand(null);
        json.setCategory(null);

        SellerProduct result = factory.create(json);

        assertThat(result.sellerBrand()).isNull();
        assertThat(result.sellerCategory()).isNull();
    }
}
