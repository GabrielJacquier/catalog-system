package com.catalog.consolidation.domain.service;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.ProductLinkedToSeller;
import com.catalog.consolidation.domain.model.Seller;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.domain.repository.ProductRepository;
import com.catalog.consolidation.domain.repository.SellerProductRepository;
import com.catalog.consolidation.domain.repository.SellerRepository;

public class ProductInsertionService {

    private final ProductNormalizationService productNormalizationService;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final SellerProductRepository sellerProductRepository;

    public ProductInsertionService(ProductNormalizationService productNormalizationService,
                                    SellerRepository sellerRepository,
                                    ProductRepository productRepository,
                                    SellerProductRepository sellerProductRepository) {
        this.productNormalizationService = productNormalizationService;
        this.sellerRepository = sellerRepository;
        this.productRepository = productRepository;
        this.sellerProductRepository = sellerProductRepository;
    }

    public ProductInsertionResult insert(SellerProduct sellerProduct) {
        try {
            return insertInternal(sellerProduct);
        } catch (Exception e) {
            return ProductInsertionResult.failure(sellerProduct, e.getMessage());
        }
    }

    private ProductInsertionResult insertInternal(SellerProduct sellerProduct) {
        Seller sellerCandidate = buildSeller(sellerProduct);
        Seller sellerFetched = sellerRepository.insertIfNotExistsAndFetch(sellerCandidate);

        Product candidate = buildProduct(sellerProduct);
        ProductInsertionResult result = productRepository.insertIfNotExistsAndFetch(candidate);
        Product productFetched = result.product();

        ProductLinkedToSeller productLinkedToSeller = new ProductLinkedToSeller(
                sellerFetched,
                productFetched,
                sellerProduct.sellerProductId(),
                sellerProduct.sellerProductName(),
                sellerProduct.sellerBrand(),
                sellerProduct.sellerCategory()
        );

        boolean linked = sellerProductRepository.link(productLinkedToSeller);

        return result.withProductLinkedToSeller(linked);
    }

    private Seller buildSeller(SellerProduct sellerProduct) {
        String name = sellerProduct.seller().getName();
        String normalizedName = productNormalizationService.normalizeSellerName(name);
        return new Seller(name, normalizedName);
    }

    private Product buildProduct(SellerProduct sellerProduct) {
        String normalizedProductName = productNormalizationService.normalizeProductName(sellerProduct.sellerProductName());
        String normalizedBrand = productNormalizationService.normalizeBrand(sellerProduct.sellerBrand());
        String category = productNormalizationService.displayCategory(sellerProduct.sellerCategory());
        String normalizedCategory = productNormalizationService.normalizeCategory(sellerProduct.sellerCategory());

        return new Product(
                sellerProduct.sellerProductName(),
                sellerProduct.sellerBrand(),
                category,
                normalizedProductName,
                normalizedBrand,
                normalizedCategory,
                Availability.PENDING
        );
    }
}
