package com.catalog.consolidation.application;

import com.catalog.consolidation.domain.model.SellerProductInput;
import com.catalog.consolidation.domain.repository.ProductRepository;
import com.catalog.consolidation.domain.repository.SellerProductRepository;
import com.catalog.consolidation.domain.service.ProductInsertionService;
import com.catalog.consolidation.domain.service.ProductMatcher;
import com.catalog.consolidation.domain.service.SellerProductPreparationService;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;
import com.catalog.consolidation.infrastructure.json.JsonCatalogReader;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SchemaMigration;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SqliteProductRepository;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SqliteSellerProductRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {

    private static final String DEFAULT_DB_PATH = "database/catalog.db";
    private static final String DEFAULT_INPUT_PATH = "input/seller-products.json";

    public static void main(String[] args) throws Exception {
        Map<String, String> arguments = parseArguments(args);
        Path databasePath = Paths.get(arguments.getOrDefault("--db", DEFAULT_DB_PATH));
        Path inputPath = Paths.get(arguments.getOrDefault("--input", DEFAULT_INPUT_PATH));

        ProductMatcher productMatcher = new ProductMatcher();
        DatabaseConfig databaseConfig = new DatabaseConfig(databasePath.toString());

        System.out.println("Stage 1: Preparing database...");
        new SchemaMigration(databaseConfig, productMatcher).run();
        System.out.println("Stage 1 completed.");

        System.out.println("Stage 2: Importing catalog from " + inputPath + "...");
        List<SellerProductInput> inputs = new JsonCatalogReader().read(inputPath);

        SellerProductPreparationService preparationService = new SellerProductPreparationService(productMatcher);
        ProductRepository productRepository = new SqliteProductRepository(databaseConfig);
        SellerProductRepository sellerProductRepository = new SqliteSellerProductRepository(databaseConfig);
        ProductInsertionService productInsertionService = new ProductInsertionService(
                preparationService,
                productRepository,
                sellerProductRepository
        );

        ImportCatalogService importCatalogService = new ImportCatalogService(productInsertionService);
        ImportCatalogResult result = importCatalogService.execute(inputs);

        System.out.println("Stage 2 completed.");
        System.out.println("Summary:");
        System.out.println("  Total processed: " + result.totalProcessed());
        System.out.println("  Products inserted: " + result.productsInserted());
        System.out.println("  Seller links created: " + result.sellerLinksCreated());
        System.out.println("  Seller links skipped: " + result.sellerLinksSkipped());
    }

    static Map<String, String> parseArguments(String[] args) {
        Map<String, String> parsed = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length) {
                parsed.put(args[i], args[i + 1]);
                i++;
            }
        }
        return parsed;
    }
}
