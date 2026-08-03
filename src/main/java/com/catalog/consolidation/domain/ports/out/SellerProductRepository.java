package com.catalog.consolidation.domain.ports.out;

import com.catalog.consolidation.domain.model.SellerProductLink;

public interface SellerProductRepository {

    boolean link(long productId, SellerProductLink sellerProductLink);
}
