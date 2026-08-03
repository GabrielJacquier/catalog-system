package com.catalog.consolidation.infrastructure.factory;

import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.infrastructure.json.SellerProductInputJson;

import java.util.List;

public class SellerProductFactory {

    public SellerProduct create(SellerProductInputJson json) {
        return new SellerProduct(
                json.getSellerName(),
                json.getId(),
                json.getName(),
                json.getBrand(),
                json.getCategory()
        );
    }

    public List<SellerProduct> createAll(List<SellerProductInputJson> jsonList) {
        return jsonList.stream()
                .map(this::create)
                .toList();
    }
}
