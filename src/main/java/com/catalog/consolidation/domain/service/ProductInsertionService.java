package com.catalog.consolidation.domain.service;

import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.ProductUpsertResult;
import com.catalog.consolidation.domain.model.SellerProductInput;
import com.catalog.consolidation.domain.model.SellerProductLink;
import com.catalog.consolidation.domain.repository.ProductRepository;
import com.catalog.consolidation.domain.repository.SellerProductRepository;

public class ProductInsertionService {

    private final SellerProductPreparationService preparationService;
    private final ProductRepository productRepository;
    private final SellerProductRepository sellerProductRepository;

    public ProductInsertionService(SellerProductPreparationService preparationService,
                                   ProductRepository productRepository,
                                   SellerProductRepository sellerProductRepository) {
        this.preparationService = preparationService;
        this.productRepository = productRepository;
        this.sellerProductRepository = sellerProductRepository;
    }

    public ProductInsertionResult insert(SellerProductInput input) {
        Product candidate = preparationService.prepareCandidate(input);
        ProductUpsertResult upsertResult = productRepository.insertIfNotExistsAndFetch(candidate);

        SellerProductLink link = preparationService.prepareLink(input);
        boolean linked = sellerProductRepository.link(upsertResult.product().getId(), link);

        return new ProductInsertionResult(upsertResult, linked);
    }
}
