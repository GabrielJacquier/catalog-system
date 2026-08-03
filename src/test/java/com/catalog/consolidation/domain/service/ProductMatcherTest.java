package com.catalog.consolidation.domain.service;

import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.SellerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMatcherTest {

    private ProductMatcher productMatcher;

    @BeforeEach
    void setUp() {
        productMatcher = new ProductMatcher();
    }

    @Test
    void shouldCollapseExtraWhitespaceInProductName() {
        assertThat(productMatcher.normalizeProductName("iPhone 15  Pro"))
                .isEqualTo("iphone 15 pro");
    }

    @Test
    void shouldRemoveAccentsFromProductName() {
        assertThat(productMatcher.normalizeProductName("Câmera Canon EOS R6"))
                .isEqualTo("camera canon eos r6");
    }

    @Test
    void shouldNormalizeQuotesInProductName() {
        assertThat(productMatcher.normalizeProductName("Tablet iPad Pro 12.9\""))
                .isEqualTo("tablet ipad pro 12.9'");
        assertThat(productMatcher.normalizeProductName("Tablet iPad Pro 12.9''"))
                .isEqualTo("tablet ipad pro 12.9'");
    }

    @Test
    void shouldTreatNullBrandAsEmptyString() {
        assertThat(productMatcher.normalizeBrand(null)).isEmpty();
    }

    @Test
    void shouldProduceSameNormalizedPairForDifferentCategories() {
        String name = "MacBook Air  M2";
        String brand = "Apple";

        String normalizedName = productMatcher.normalizeProductName(name);
        String normalizedBrand = productMatcher.normalizeBrand(brand);

        assertThat(normalizedName).isEqualTo("macbook air m2");
        assertThat(normalizedBrand).isEqualTo("apple");
    }

    @Test
    void shouldDetectSameProductByNormalizedFields() {
        Product first = new Product(
                "Smartphone Galaxy S23", "Samsung", "Electronics",
                productMatcher.normalizeProductName("Smartphone Galaxy S23"),
                productMatcher.normalizeBrand("Samsung"),
                SellerStatus.ACTIVE_TO_SELLER
        );
        Product second = new Product(
                "Smartphone  Galaxy S23", "Samsung", "Phones",
                productMatcher.normalizeProductName("Smartphone  Galaxy S23"),
                productMatcher.normalizeBrand("Samsung"),
                SellerStatus.INACTIVE_TO_SELLER
        );

        assertThat(productMatcher.isSameProduct(first, second)).isTrue();
    }
}
