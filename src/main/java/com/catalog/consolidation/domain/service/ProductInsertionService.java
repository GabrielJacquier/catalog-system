package com.catalog.consolidation.domain.service;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.domain.repository.ProductRepository;
import com.catalog.consolidation.domain.repository.SellerProductRepository;

public class ProductInsertionService {

    private final ProductNormalizationService productNormalizationService;
    private final ProductRepository productRepository;
    private final SellerProductRepository sellerProductRepository;

    public ProductInsertionService(ProductNormalizationService productNormalizationService,
                                    ProductRepository productRepository,
                                    SellerProductRepository sellerProductRepository) {
        this.productNormalizationService = productNormalizationService;
        this.productRepository = productRepository;
        this.sellerProductRepository = sellerProductRepository;
    }

    public ProductInsertionResult insert(SellerProduct sellerProduct) {
        Product candidate = buildProduct(sellerProduct);
        ProductInsertionResult result = productRepository.insertIfNotExistsAndFetch(candidate);

        boolean productLinkedToSeller = sellerProductRepository.link(result.product().getId(), sellerProduct);

        return result.withProductLinkedToSeller(productLinkedToSeller);
    }

    private Product buildProduct(SellerProduct sellerProduct) {
        String normalizedProductName = productNormalizationService.normalizeProductName(sellerProduct.sellerProductName());
        String normalizedBrand = productNormalizationService.normalizeBrand(sellerProduct.sellerBrand());
        String category = productNormalizationService.normalizeCategory(sellerProduct.sellerCategory());

        return new Product(
                sellerProduct.sellerProductName(),
                sellerProduct.sellerBrand(),
                category,
                normalizedProductName,
                normalizedBrand,
                Availability.PENDING
        );
    }
}
