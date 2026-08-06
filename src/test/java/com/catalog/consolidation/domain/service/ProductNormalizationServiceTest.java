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
    void shouldTrimLeadingAndTrailingWhitespace() {
        assertThat(productNormalizationService.normalizeProductName("  iPhone 15 Pro  "))
                .isEqualTo("iphone 15 pro");
    }

    @Test
    void shouldCollapseExtraWhitespaceInProductName() {
        assertThat(productNormalizationService.normalizeProductName("iPhone 15  Pro"))
                .isEqualTo("iphone 15 pro");
    }

    @Test
    void shouldConvertToLowerCase() {
        assertThat(productNormalizationService.normalizeBrand("SAMSUNG"))
                .isEqualTo("samsung");
    }

    @Test
    void shouldRemoveAccentsFromProductName() {
        assertThat(productNormalizationService.normalizeProductName("Câmera Canon EOS R6"))
                .isEqualTo("camera canon eos r6");
    }

    @Test
    void shouldNormalizeDoubleQuoteToSingleQuote() {
        assertThat(productNormalizationService.normalizeProductName("Tablet iPad Pro 12.9\""))
                .isEqualTo("tablet ipad pro 12.9'");
    }

    @Test
    void shouldNormalizeDoubleSingleQuoteToSingleQuote() {
        assertThat(productNormalizationService.normalizeProductName("Tablet iPad Pro 12.9''"))
                .isEqualTo("tablet ipad pro 12.9'");
    }

    @Test
    void shouldTreatNullProductNameAsEmptyString() {
        assertThat(productNormalizationService.normalizeProductName(null)).isEmpty();
    }

    @Test
    void shouldTreatNullBrandAsEmptyString() {
        assertThat(productNormalizationService.normalizeBrand(null)).isEmpty();
    }

    @Test
    void shouldNormalizeCategoryLikeNameAndBrand() {
        assertThat(productNormalizationService.normalizeCategory("  Electronics  "))
                .isEqualTo("electronics");
    }

    @Test
    void shouldTreatNullCategoryAsEmptyString() {
        assertThat(productNormalizationService.normalizeCategory(null)).isEmpty();
    }

    @Test
    void shouldTreatBlankCategoryAsEmptyString() {
        assertThat(productNormalizationService.normalizeCategory("   ")).isEmpty();
    }

    @Test
    void shouldTrimDisplayCategoryWithoutChangingCase() {
        assertThat(productNormalizationService.displayCategory("  Electronics  "))
                .isEqualTo("Electronics");
    }

    @Test
    void shouldReturnNullDisplayCategoryForNullOrBlank() {
        assertThat(productNormalizationService.displayCategory(null)).isNull();
        assertThat(productNormalizationService.displayCategory("   ")).isNull();
    }

    @Test
    void shouldTreatNullSellerNameAsEmptyString() {
        assertThat(productNormalizationService.normalizeSellerName(null)).isEmpty();
    }

    @Test
    void shouldNormalizeSellerNameToUpperCase() {
        assertThat(productNormalizationService.normalizeSellerName("  MegaStore  "))
                .isEqualTo("MEGASTORE");
    }

    @Test
    void shouldDetectSameProductByNormalizedFieldsIncludingCategory() {
        Product first = product(
                "Smartphone Galaxy S23", "Samsung", "Electronics",
                "smartphone galaxy s23", "samsung", "electronics",
                Availability.AVAILABLE
        );
        Product second = product(
                "Smartphone  Galaxy S23", "Samsung", " electronics ",
                "smartphone galaxy s23", "samsung", "electronics",
                Availability.PENDING
        );

        assertThat(productNormalizationService.isSameProduct(first, second)).isTrue();
    }

    @Test
    void shouldDetectDifferentProductsWhenCategoryDiffers() {
        Product first = product(
                "Smartphone Galaxy S23", "Samsung", "Electronics",
                "smartphone galaxy s23", "samsung", "electronics",
                Availability.AVAILABLE
        );
        Product second = product(
                "Smartphone Galaxy S23", "Samsung", "Phones",
                "smartphone galaxy s23", "samsung", "phones",
                Availability.PENDING
        );

        assertThat(productNormalizationService.isSameProduct(first, second)).isFalse();
    }

    @Test
    void shouldDetectDifferentProductsByNormalizedFields() {
        Product first = product(
                "Smartphone Galaxy S23", "Samsung", "Electronics",
                "smartphone galaxy s23", "samsung", "electronics",
                Availability.AVAILABLE
        );
        Product second = product(
                "Smartphone Galaxy S24", "Samsung", "Electronics",
                "smartphone galaxy s24", "samsung", "electronics",
                Availability.PENDING
        );

        assertThat(productNormalizationService.isSameProduct(first, second)).isFalse();
    }

    @Test
    void shouldTreatBlankBrandAsEmptyAndDistinctBrandsAsDifferentProducts() {
        assertThat(productNormalizationService.normalizeBrand("   ")).isEmpty();

        Product samsung = product(
                "Galaxy S23", "Samsung", "Electronics",
                "galaxy s23", "samsung", "electronics",
                Availability.AVAILABLE
        );
        Product apple = product(
                "Galaxy S23", "Apple", "Electronics",
                "galaxy s23", "apple", "electronics",
                Availability.PENDING
        );
        assertThat(productNormalizationService.isSameProduct(samsung, apple)).isFalse();
    }

    @Test
    void shouldTreatNullCategoriesAsSameWhenNameAndBrandMatch() {
        Product first = product(
                "Generic Widget", null, null,
                "generic widget", "", "",
                Availability.AVAILABLE
        );
        Product second = product(
                "Generic  Widget", null, null,
                "generic widget", "", "",
                Availability.PENDING
        );

        assertThat(productNormalizationService.isSameProduct(first, second)).isTrue();
    }

    @Test
    void shouldKeepTrademarkAndAmpersandCharactersWhenNormalizingProductName() {
        assertThat(productNormalizationService.normalizeProductName("Router® Wi-Fi & Mesh"))
                .isEqualTo("router® wi-fi & mesh");
    }

    private static Product product(String name, String brand, String category,
                                   String normalizedName, String normalizedBrand, String normalizedCategory,
                                   Availability availability) {
        return new Product(name, brand, category, normalizedName, normalizedBrand, normalizedCategory, availability);
    }
}
