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
        if (category == null) {
            return null;
        }
        String trimmed = category.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public boolean isSameProduct(Product first, Product second) {
        return Objects.equals(first.getNormalizedProductName(), second.getNormalizedProductName())
                && Objects.equals(first.getNormalizedBrand(), second.getNormalizedBrand());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        normalized = normalized.replace("''", "'").replace("\"", "'");
        return normalized.toLowerCase(Locale.ROOT);
    }
}
