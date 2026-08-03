package com.catalog.consolidation.infrastructure.factory;

import com.catalog.consolidation.domain.model.SellerProductInput;
import com.catalog.consolidation.infrastructure.json.SellerProductInputJson;

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
