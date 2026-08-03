package com.catalog.consolidation.domain.service;

import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.Availability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductNormalizationServiceTest {

    private ProductNormalizationService productNormalizationService;

    @BeforeEach
    void setUp() {
        productNormalizationService = new ProductNormalizationService();
    }

    @Test
    void shouldCollapseExtraWhitespaceInProductName() {
        assertThat(productNormalizationService.normalizeProductName("iPhone 15  Pro"))
                .isEqualTo("iphone 15 pro");
    }

    @Test
    void shouldRemoveAccentsFromProductName() {
        assertThat(productNormalizationService.normalizeProductName("Câmera Canon EOS R6"))
                .isEqualTo("camera canon eos r6");
    }

    @Test
    void shouldNormalizeQuotesInProductName() {
        assertThat(productNormalizationService.normalizeProductName("Tablet iPad Pro 12.9\""))
                .isEqualTo("tablet ipad pro 12.9'");
        assertThat(productNormalizationService.normalizeProductName("Tablet iPad Pro 12.9''"))
                .isEqualTo("tablet ipad pro 12.9'");
    }

    @Test
    void shouldTreatNullBrandAsEmptyString() {
        assertThat(productNormalizationService.normalizeBrand(null)).isEmpty();
    }

    @Test
    void shouldProduceSameNormalizedPairForDifferentCategories() {
        String name = "MacBook Air  M2";
        String brand = "Apple";

        String normalizedName = productNormalizationService.normalizeProductName(name);
        String normalizedBrand = productNormalizationService.normalizeBrand(brand);

        assertThat(normalizedName).isEqualTo("macbook air m2");
        assertThat(normalizedBrand).isEqualTo("apple");
    }

    @Test
    void shouldDetectSameProductByNormalizedFields() {
        Product first = new Product(
                "Smartphone Galaxy S23", "Samsung", "Electronics",
                productNormalizationService.normalizeProductName("Smartphone Galaxy S23"),
                productNormalizationService.normalizeBrand("Samsung"),
                Availability.AVAILABLE
        );
        Product second = new Product(
                "Smartphone  Galaxy S23", "Samsung", "Phones",
                productNormalizationService.normalizeProductName("Smartphone  Galaxy S23"),
                productNormalizationService.normalizeBrand("Samsung"),
                Availability.PENDING
        );

        assertThat(productNormalizationService.isSameProduct(first, second)).isTrue();
    }
}
