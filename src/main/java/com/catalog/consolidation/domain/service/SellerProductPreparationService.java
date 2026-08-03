package com.catalog.consolidation.domain.service;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.SellerProductInput;
import com.catalog.consolidation.domain.model.SellerProductLink;

public class SellerProductPreparationService {

    private final ProductMatcher productMatcher;

    public SellerProductPreparationService(ProductMatcher productMatcher) {
        this.productMatcher = productMatcher;
    }

    public Product prepareCandidate(SellerProductInput input) {
        String normalizedProductName = productMatcher.normalizeProductName(input.name());
        String normalizedBrand = productMatcher.normalizeBrand(input.brand());
        String category = productMatcher.normalizeCategory(input.category());

        return new Product(
                input.name(),
                input.brand(),
                category,
                normalizedProductName,
                normalizedBrand,
                Availability.PENDING
        );
    }

    public SellerProductLink prepareLink(SellerProductInput input) {
        return new SellerProductLink(
                input.sellerName(),
                input.id(),
                input.name(),
                input.brand(),
                input.category()
        );
    }
}
