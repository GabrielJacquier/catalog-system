package com.catalog.consolidation.domain.repository;

import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductUpsertResult;

public interface ProductRepository {

    ProductUpsertResult insertIfNotExistsAndFetch(Product product);
}
