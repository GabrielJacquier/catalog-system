package com.catalog.consolidation.infrastructure.json;

import com.catalog.consolidation.domain.model.SellerProductInput;

public class SellerProductInputFactory {

    public SellerProductInput create(SellerProductInputJson json) {
        return new SellerProductInput(
                json.getId(),
                json.getSellerName(),
                json.getName(),
                json.getBrand(),
                json.getCategory()
        );
    }
}
