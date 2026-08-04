package com.catalog.consolidation.application;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.model.Seller;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.domain.repository.ProductRepository;
import com.catalog.consolidation.domain.repository.SellerProductRepository;
import com.catalog.consolidation.domain.repository.SellerRepository;
import com.catalog.consolidation.domain.service.ProductInsertionService;
import com.catalog.consolidation.domain.service.ProductNormalizationService;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;
import com.catalog.consolidation.infrastructure.json.FailedSellerProductOutputJson;
import com.catalog.consolidation.infrastructure.json.JsonCatalogReader;
import com.catalog.consolidation.infrastructure.json.JsonFailedCatalogWriter;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SchemaMigration;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SqliteProductRepository;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SqliteSellerProductRepository;
import com.catalog.consolidation.infrastructure.persistence.sqlite.SqliteSellerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogIntegrationAppIT {

    @TempDir
    Path tempDir;

    private DatabaseConfig databaseConfig;
    private CatalogIntegrationApp catalogIntegrationApp;

    @BeforeEach
    void setUp() throws Exception {
        Path databasePath = tempDir.resolve("catalog.db");
        createSeedDatabase(databasePath);
        databaseConfig = new DatabaseConfig(databasePath.toString());
        ProductNormalizationService productNormalizationService = new ProductNormalizationService();
        SchemaMigration schemaMigration = new SchemaMigration(databaseConfig, productNormalizationService);
        schemaMigration.run();

        ProductRepository productRepository = new SqliteProductRepository(databaseConfig);
        SellerRepository sellerRepository = new SqliteSellerRepository(databaseConfig);
        SellerProductRepository sellerProductRepository = new SqliteSellerProductRepository(databaseConfig);
        ProductInsertionService productInsertionService = new ProductInsertionService(
                productNormalizationService,
                sellerRepository,
                productRepository,
                sellerProductRepository
        );

        catalogIntegrationApp = new CatalogIntegrationApp(
                schemaMigration,
                new JsonCatalogReader(),
                productInsertionService,
                new JsonFailedCatalogWriter(),
                tempDir.resolve("failed-seller-products.json")
        );
    }

    @Test
    void shouldReuseActiveProductWithoutChangingStatus() throws Exception {
        SellerProduct sellerProduct = createSellerProduct(
                "dup-1", "MegaStore", "Smartphone  Galaxy S23", "Samsung", "Electronics"
        );

        CatalogIntegrationResult result = catalogIntegrationApp.processProducts(List.of(sellerProduct));

        assertThat(result.productsInserted()).isZero();
        assertThat(result.sellerLinksCreated()).isEqualTo(1);

        try (Connection connection = databaseConfig.getConnection();
             Statement productsStatement = connection.createStatement();
             Statement sellerProductsStatement = connection.createStatement();
             ResultSet products = productsStatement.executeQuery("SELECT COUNT(*) AS total FROM Product");
             ResultSet sellerProducts = sellerProductsStatement.executeQuery("SELECT COUNT(*) AS total FROM SellerProduct")) {
            products.next();
            sellerProducts.next();
            assertThat(products.getInt("total")).isEqualTo(1);
            assertThat(sellerProducts.getInt("total")).isEqualTo(1);
        }

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet product = statement.executeQuery(
                     "SELECT Availability FROM Product WHERE Name = 'Smartphone Galaxy S23'")) {
            product.next();
            assertThat(product.getString("Availability")).isEqualTo(Availability.AVAILABLE.name());
        }
    }

    @Test
    void shouldInsertNewProductAsInactive() throws Exception {
        SellerProduct sellerProduct = createSellerProduct(
                "new-1", "MegaStore", "Brand New Product", "Acme", "Gadgets"
        );

        CatalogIntegrationResult result = catalogIntegrationApp.processProducts(List.of(sellerProduct));

        assertThat(result.productsInserted()).isEqualTo(1);

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet product = statement.executeQuery(
                     "SELECT Availability FROM Product WHERE Name = 'Brand New Product'")) {
            product.next();
            assertThat(product.getString("Availability")).isEqualTo(Availability.PENDING.name());
        }
    }

    @Test
    void shouldLinkMultipleSellersToSameProduct() throws Exception {
        SellerProduct first = createSellerProduct(
                "seller-a", "StoreA", "Smartphone  Galaxy S23", "Samsung", "Electronics"
        );
        SellerProduct second = createSellerProduct(
                "seller-b", "StoreB", "Smartphone Galaxy S23", "Samsung", "Phones"
        );

        catalogIntegrationApp.processProducts(List.of(first, second));

        try (Connection connection = databaseConfig.getConnection();
             Statement productsStatement = connection.createStatement();
             Statement sellerProductsStatement = connection.createStatement();
             ResultSet products = productsStatement.executeQuery("SELECT COUNT(*) AS total FROM Product");
             ResultSet sellerProducts = sellerProductsStatement.executeQuery("SELECT COUNT(*) AS total FROM SellerProduct")) {
            products.next();
            sellerProducts.next();
            assertThat(products.getInt("total")).isEqualTo(1);
            assertThat(sellerProducts.getInt("total")).isEqualTo(2);
        }
    }

    @Test
    void shouldBeIdempotentWhenReprocessingSameFile() throws Exception {
        SellerProduct sellerProduct = createSellerProduct(
                "idem-1", "MegaStore", "Brand New Product", "Acme", "Gadgets"
        );

        catalogIntegrationApp.processProducts(List.of(sellerProduct));
        CatalogIntegrationResult secondRun = catalogIntegrationApp.processProducts(List.of(sellerProduct));

        assertThat(secondRun.productsInserted()).isZero();
        assertThat(secondRun.sellerLinksCreated()).isZero();
        assertThat(secondRun.sellerLinksSkipped()).isEqualTo(1);
    }

    @Test
    void shouldWriteFailedItemToErrorsOutputAndContinueImportingRemainingOnes() throws Exception {
        Path errorsOutput = tempDir.resolve("failed-seller-products.json");
        Path input = tempDir.resolve("seller-products.json");
        Files.writeString(input, """
                [
                  {
                    "Id": "fail-1",
                    "SellerName": "StoreA",
                    "Name": "Broken Product",
                    "Brand": "Acme",
                    "Category": "Gadgets"
                  },
                  {
                    "Id": "ok-1",
                    "SellerName": "StoreB",
                    "Name": "Good Product",
                    "Brand": "Acme",
                    "Category": "Gadgets"
                  }
                ]
                """);

        ProductNormalizationService productNormalizationService = new ProductNormalizationService();
        SchemaMigration schemaMigration = new SchemaMigration(databaseConfig, productNormalizationService);
        ProductRepository realProductRepository = new SqliteProductRepository(databaseConfig);
        ProductRepository productRepository = product -> {
            if ("Broken Product".equals(product.getName())) {
                throw new IllegalStateException("simulated persistence failure");
            }
            return realProductRepository.insertIfNotExistsAndFetch(product);
        };
        ProductInsertionService productInsertionService = new ProductInsertionService(
                productNormalizationService,
                new SqliteSellerRepository(databaseConfig),
                productRepository,
                new SqliteSellerProductRepository(databaseConfig)
        );

        CatalogIntegrationApp app = new CatalogIntegrationApp(
                schemaMigration,
                new JsonCatalogReader(),
                productInsertionService,
                new JsonFailedCatalogWriter(),
                errorsOutput
        );

        app.startApp(input);

        assertThat(errorsOutput).exists();
        List<FailedSellerProductOutputJson> failedItems = new ObjectMapper().readValue(
                errorsOutput.toFile(),
                new TypeReference<>() {
                }
        );
        assertThat(failedItems).hasSize(1);
        FailedSellerProductOutputJson failedItem = failedItems.get(0);
        assertThat(failedItem.getId()).isEqualTo("fail-1");
        assertThat(failedItem.getSellerName()).isEqualTo("StoreA");
        assertThat(failedItem.getName()).isEqualTo("Broken Product");
        assertThat(failedItem.getBrand()).isEqualTo("Acme");
        assertThat(failedItem.getCategory()).isEqualTo("Gadgets");
        assertThat(failedItem.getErrorMessage()).isEqualTo("simulated persistence failure");

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet goodProduct = statement.executeQuery(
                    "SELECT COUNT(*) AS total FROM Product WHERE Name = 'Good Product'")) {
                goodProduct.next();
                assertThat(goodProduct.getInt("total")).isEqualTo(1);
            }
            try (ResultSet brokenProduct = statement.executeQuery(
                    "SELECT COUNT(*) AS total FROM Product WHERE Name = 'Broken Product'")) {
                brokenProduct.next();
                assertThat(brokenProduct.getInt("total")).isZero();
            }
            try (ResultSet sellerProducts = statement.executeQuery(
                    "SELECT COUNT(*) AS total FROM SellerProduct WHERE SellerProductId = 'ok-1'")) {
                sellerProducts.next();
                assertThat(sellerProducts.getInt("total")).isEqualTo(1);
            }
        }
    }

    private void createSeedDatabase(Path databasePath) throws Exception {
        DatabaseConfig config = new DatabaseConfig(databasePath.toString());
        try (Connection connection = config.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE Product (
                        Id INTEGER PRIMARY KEY AUTOINCREMENT,
                        Name TEXT NOT NULL,
                        Brand TEXT,
                        Category TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE SellerProduct (
                        Id INTEGER PRIMARY KEY AUTOINCREMENT,
                        SellerName TEXT NOT NULL,
                        ProductId INTEGER NOT NULL,
                        SellerProductId INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO Product (Name, Brand, Category)
                    VALUES ('Smartphone Galaxy S23', 'Samsung', 'Electronics')
                    """);
        }
    }

    private SellerProduct createSellerProduct(String sellerProductId, String sellerName, String sellerProductName,
                                              String sellerBrand, String sellerCategory) {
        return new SellerProduct(
                new Seller(sellerName, null),
                sellerProductId,
                sellerProductName,
                sellerBrand,
                sellerCategory
        );
    }
}
