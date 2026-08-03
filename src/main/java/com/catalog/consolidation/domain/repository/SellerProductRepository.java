package com.catalog.consolidation.domain.repository;

import com.catalog.consolidation.domain.model.SellerProductLink;

public interface SellerProductRepository {

    boolean link(long productId, SellerProductLink sellerProductLink);
}
