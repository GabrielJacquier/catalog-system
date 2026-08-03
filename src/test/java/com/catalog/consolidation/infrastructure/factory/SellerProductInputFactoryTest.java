package com.catalog.consolidation.infrastructure.factory;

import com.catalog.consolidation.domain.model.SellerProductInput;
import com.catalog.consolidation.infrastructure.json.SellerProductInputJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SellerProductInputFactoryTest {

    private final SellerProductInputFactory factory = new SellerProductInputFactory();

    @Test
    void shouldCreateDomainModelFromJsonDto() {
        SellerProductInputJson json = new SellerProductInputJson();
        json.setId("seller-1");
        json.setSellerName("MegaStore");
        json.setName("Smartphone Galaxy S23");
        json.setBrand("Samsung");
        json.setCategory("Electronics");

        SellerProductInput result = factory.create(json);

        assertThat(result.id()).isEqualTo("seller-1");
        assertThat(result.sellerName()).isEqualTo("MegaStore");
        assertThat(result.name()).isEqualTo("Smartphone Galaxy S23");
        assertThat(result.brand()).isEqualTo("Samsung");
        assertThat(result.category()).isEqualTo("Electronics");
    }

    @Test
    void shouldHandleNullBrandAndCategory() {
        SellerProductInputJson json = new SellerProductInputJson();
        json.setId("seller-2");
        json.setSellerName("StoreB");
        json.setName("Generic Product");
        json.setBrand(null);
        json.setCategory(null);

        SellerProductInput result = factory.create(json);

        assertThat(result.brand()).isNull();
        assertThat(result.category()).isNull();
    }
}
