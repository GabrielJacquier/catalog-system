package com.catalog.consolidation.application.mapper;

import com.catalog.consolidation.application.dto.SellerProductInput;
import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.SellerProductLink;
import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.service.ProductMatcher;

public class ProductMapper {

    public Product toProduct(SellerProductInput input, ProductMatcher productMatcher) {
        String normalizedProductName = productMatcher.normalizeProductName(input.getName());
        String normalizedBrand = productMatcher.normalizeBrand(input.getBrand());
        String category = productMatcher.normalizeCategory(input.getCategory());

        return new Product(
                input.getName(),
                input.getBrand(),
                category,
                normalizedProductName,
                normalizedBrand,
                Availability.PENDING
        );
    }

    public SellerProductLink toSellerProductLink(SellerProductInput input) {
        return new SellerProductLink(
                input.getSellerName(),
                input.getId(),
                input.getName(),
                input.getBrand(),
                input.getCategory()
        );
    }
}
