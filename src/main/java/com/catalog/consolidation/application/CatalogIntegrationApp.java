package com.catalog.consolidation.application;

import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.domain.service.ProductInsertionService;
import com.catalog.consolidation.infrastructure.json.JsonCatalogReader;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SchemaMigration;

import java.nio.file.Path;
import java.util.List;

public class CatalogIntegrationApp {

    private final SchemaMigration schemaMigration;
    private final JsonCatalogReader jsonCatalogReader;
    private final ProductInsertionService productInsertionService;

    public CatalogIntegrationApp(SchemaMigration schemaMigration,
                                 JsonCatalogReader jsonCatalogReader,
                                 ProductInsertionService productInsertionService) {
        this.schemaMigration = schemaMigration;
        this.jsonCatalogReader = jsonCatalogReader;
        this.productInsertionService = productInsertionService;
    }

    public void startApp(Path inputPath) throws Exception {
        System.out.println("Stage 1: Preparing database...");
        schemaMigration.run();
        System.out.println("Stage 1 completed.");

        System.out.println("Stage 2: Importing catalog from " + inputPath + "...");
        List<SellerProduct> sellerProducts = jsonCatalogReader.read(inputPath);

        CatalogIntegrationResult result = processProducts(sellerProducts);

        printSummary(result);
    }

    CatalogIntegrationResult processProducts(List<SellerProduct> sellerProducts) {
        int productsInserted = 0;
        int sellerLinksCreated = 0;
        int sellerLinksSkipped = 0;

        for (SellerProduct sellerProduct : sellerProducts) {
            ProductInsertionResult result = productInsertionService.insert(sellerProduct);

            if (result.inserted()) {
                productsInserted++;
            }

            if (result.productLinkedToSeller()) {
                sellerLinksCreated++;
            } else {
                sellerLinksSkipped++;
            }
        }

        return new CatalogIntegrationResult(
                productsInserted,
                sellerLinksCreated,
                sellerLinksSkipped,
                sellerProducts.size()
        );
    }

    private void printSummary(CatalogIntegrationResult result) {
        System.out.println("Stage 2 completed.");
        System.out.println("Summary:");
        System.out.println("  Total processed: " + result.totalProcessed());
        System.out.println("  Products inserted: " + result.productsInserted());
        System.out.println("  Seller links created: " + result.sellerLinksCreated());
        System.out.println("  Seller links skipped: " + result.sellerLinksSkipped());
    }
}
