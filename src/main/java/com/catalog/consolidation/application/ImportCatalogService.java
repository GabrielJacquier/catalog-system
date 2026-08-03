package com.catalog.consolidation.application;

import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.SellerProductInput;
import com.catalog.consolidation.domain.service.ProductInsertionService;

import java.util.List;

public class ImportCatalogService {

    private final ProductInsertionService productInsertionService;

    public ImportCatalogService(ProductInsertionService productInsertionService) {
        this.productInsertionService = productInsertionService;
    }

    public ImportCatalogResult execute(List<SellerProductInput> inputs) {
        int productsInserted = 0;
        int sellerLinksCreated = 0;
        int sellerLinksSkipped = 0;

        for (SellerProductInput input : inputs) {
            ProductInsertionResult result = productInsertionService.insert(input);

            if (result.upsertResult().inserted()) {
                productsInserted++;
            }

            if (result.linked()) {
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
