package com.catalog.consolidation.application.service;

import com.catalog.consolidation.application.dto.SellerProductInput;
import com.catalog.consolidation.application.mapper.ProductMapper;
import com.catalog.consolidation.domain.model.ImportCatalogResult;
import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductUpsertResult;
import com.catalog.consolidation.domain.service.ProductMatcher;
import com.catalog.consolidation.domain.ports.in.ImportCatalogUseCase;
import com.catalog.consolidation.domain.ports.out.ProductRepository;
import com.catalog.consolidation.domain.ports.out.SellerProductRepository;

import java.util.List;

public class ImportCatalogService implements ImportCatalogUseCase {

    private final ProductRepository productRepository;
    private final SellerProductRepository sellerProductRepository;
    private final ProductMapper productMapper;
    private final ProductMatcher productMatcher;

    public ImportCatalogService(ProductRepository productRepository,
                                SellerProductRepository sellerProductRepository,
                                ProductMapper productMapper,
                                ProductMatcher productMatcher) {
        this.productRepository = productRepository;
        this.sellerProductRepository = sellerProductRepository;
        this.productMapper = productMapper;
        this.productMatcher = productMatcher;
    }

    @Override
    public ImportCatalogResult execute(List<SellerProductInput> inputs) {
        int productsInserted = 0;
        int sellerLinksCreated = 0;
        int sellerLinksSkipped = 0;

        for (SellerProductInput input : inputs) {
            Product candidate = productMapper.toProduct(input, productMatcher);
            ProductUpsertResult upsertResult = productRepository.insertIfNotExistsAndFetch(candidate);
            if (upsertResult.inserted()) {
                productsInserted++;
            }

            boolean linked = sellerProductRepository.link(
                    upsertResult.product().getId(),
                    productMapper.toSellerProductLink(input)
            );
            if (linked) {
                sellerLinksCreated++;
            } else {
                sellerLinksSkipped++;
            }
        }

        return new ImportCatalogResult(
                productsInserted,
                sellerLinksCreated,
                sellerLinksSkipped,
                inputs.size()
        );
    }
}
