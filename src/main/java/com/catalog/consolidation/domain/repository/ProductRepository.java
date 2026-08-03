package com.catalog.consolidation.domain.repository;

import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductInsertionResult;

public interface ProductRepository {

    ProductInsertionResult insertIfNotExistsAndFetch(Product product);
}
