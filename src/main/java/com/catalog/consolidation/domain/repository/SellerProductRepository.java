package com.catalog.consolidation.domain.repository;

import com.catalog.consolidation.domain.model.SellerProduct;

public interface SellerProductRepository {

    boolean link(long productId, SellerProduct sellerProduct);
}
