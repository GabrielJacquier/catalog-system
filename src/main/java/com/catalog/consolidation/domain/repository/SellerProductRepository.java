package com.catalog.consolidation.domain.repository;

import com.catalog.consolidation.domain.model.ProductLinkedToSeller;

public interface SellerProductRepository {

    boolean link(ProductLinkedToSeller productLinkedToSeller);
}
