package com.catalog.consolidation.domain.repository;

import com.catalog.consolidation.domain.model.Seller;

public interface SellerRepository {

    Seller insertIfNotExistsAndFetch(Seller seller);
}
