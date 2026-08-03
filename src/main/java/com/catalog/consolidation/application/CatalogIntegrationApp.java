package com.catalog.consolidation.application;

import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.SellerProductInput;
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
        List<SellerProductInput> inputs = jsonCatalogReader.read(inputPath);

        CatalogIntegrationResult result = processProducts(inputs);

        printSummary(result);
    }

    CatalogIntegrationResult processProducts(List<SellerProductInput> inputs) {
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

        return new CatalogIntegrationResult(
                productsInserted,
                sellerLinksCreated,
                sellerLinksSkipped,
                inputs.size()
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
