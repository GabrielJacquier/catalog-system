package com.catalog.consolidation.infrastructure.factory;

import com.catalog.consolidation.domain.model.Seller;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.infrastructure.json.SellerProductInputJson;

import java.util.List;

public class SellerProductFactory {

    public SellerProduct create(SellerProductInputJson json) {
        Seller seller = new Seller(json.getSellerName(), null);
        return new SellerProduct(
                seller,
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
