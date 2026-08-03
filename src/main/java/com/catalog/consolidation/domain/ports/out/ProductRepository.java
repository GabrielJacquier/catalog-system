package com.catalog.consolidation.domain.ports.out;

import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductUpsertResult;

public interface ProductRepository {

    ProductUpsertResult insertIfNotExistsAndFetch(Product product);
}
