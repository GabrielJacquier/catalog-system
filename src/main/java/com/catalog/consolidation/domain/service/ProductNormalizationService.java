package com.catalog.consolidation.domain.service;

import com.catalog.consolidation.domain.model.Product;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public class ProductNormalizationService {

    public String normalizeProductName(String name) {
        return normalize(name);
    }

    public String normalizeBrand(String brand) {
        return normalize(brand);
    }

    public String normalizeCategory(String category) {
        return normalize(category);
    }

    public String displayCategory(String category) {
        if (category == null) {
            return null;
        }
        String trimmed = category.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String normalizeSellerName(String sellerName) {
        if (sellerName == null) {
            return "";
        }
        return sellerName.trim().toUpperCase(Locale.ROOT);
    }

    public boolean isSameProduct(Product first, Product second) {
        return Objects.equals(first.getNormalizedProductName(), second.getNormalizedProductName())
                && Objects.equals(first.getNormalizedBrand(), second.getNormalizedBrand())
                && Objects.equals(first.getNormalizedCategory(), second.getNormalizedCategory());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = collapseWhitespace(value);
        normalized = removeAccents(normalized);
        normalized = normalizeQuotes(normalized);
        return toLowerCase(normalized);
    }

    private String collapseWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String removeAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private String normalizeQuotes(String value) {
        return value.replace("''", "'").replace("\"", "'");
    }

    private String toLowerCase(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
