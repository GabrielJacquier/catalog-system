package com.catalog.consolidation.application;

import com.catalog.consolidation.domain.repository.ProductRepository;
import com.catalog.consolidation.domain.repository.SellerProductRepository;
import com.catalog.consolidation.domain.repository.SellerRepository;
import com.catalog.consolidation.domain.service.ProductInsertionService;
import com.catalog.consolidation.domain.service.ProductNormalizationService;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;
import com.catalog.consolidation.infrastructure.json.JsonCatalogReader;
import com.catalog.consolidation.infrastructure.json.JsonFailedCatalogWriter;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SchemaMigration;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SqliteProductRepository;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SqliteSellerProductRepository;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SqliteSellerRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Application {

    private static final String DEFAULT_SEED_DB_PATH = "samples/catalog.db";
    private static final String DEFAULT_INPUT_PATH = "samples/seller-products.json";
    private static final String DEFAULT_OUTPUT_PATH = "catalog-updated.db";
    private static final String DEFAULT_ERRORS_OUTPUT_PATH = "failed-seller-products.json";

    public static void main(String[] args) throws Exception {
        Map<String, String> arguments = parseArguments(args);
        Path seedDbPath = Paths.get(arguments.getOrDefault("--db", DEFAULT_SEED_DB_PATH));
        Path inputPath = Paths.get(arguments.getOrDefault("--input", DEFAULT_INPUT_PATH));
        Path outputPath = Paths.get(arguments.getOrDefault("--output", DEFAULT_OUTPUT_PATH));
        Path errorsOutputPath = Paths.get(arguments.getOrDefault("--errors-output", DEFAULT_ERRORS_OUTPUT_PATH));

        ensureWorkingDatabaseExists(seedDbPath, outputPath);
        System.out.println("Using working database: " + outputPath);

        ProductNormalizationService productNormalizationService = new ProductNormalizationService();
        DatabaseConfig databaseConfig = new DatabaseConfig(outputPath.toString());

        SchemaMigration schemaMigration = new SchemaMigration(databaseConfig, productNormalizationService);
        JsonCatalogReader jsonCatalogReader = new JsonCatalogReader();
        JsonFailedCatalogWriter jsonFailedCatalogWriter = new JsonFailedCatalogWriter();

        ProductRepository productRepository = new SqliteProductRepository(databaseConfig);
        SellerRepository sellerRepository = new SqliteSellerRepository(databaseConfig);
        SellerProductRepository sellerProductRepository = new SqliteSellerProductRepository(databaseConfig);
        ProductInsertionService productInsertionService = new ProductInsertionService(
                productNormalizationService,
                sellerRepository,
                productRepository,
                sellerProductRepository
        );

        CatalogIntegrationApp catalogIntegrationApp = new CatalogIntegrationApp(
                schemaMigration,
                jsonCatalogReader,
                productInsertionService,
                jsonFailedCatalogWriter,
                errorsOutputPath
        );

        catalogIntegrationApp.startApp(inputPath);
    }

    private static void ensureWorkingDatabaseExists(Path seedDbPath, Path outputPath) throws IOException {
        if (Files.notExists(outputPath)) {
            Files.copy(seedDbPath, outputPath);
        }
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
