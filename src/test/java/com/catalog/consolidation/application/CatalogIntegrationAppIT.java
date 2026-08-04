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
    void shouldInsertNewProductAsPending() throws Exception {
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
    void shouldKeepCanonicalFirstWinsAndStoreSellerSnapshotOnMatch() throws Exception {
        SellerProduct listing = createSellerProduct(
                "snap-1", "MegaStore", "Smartphone  Galaxy S23", "SAMSUNG", "Phones"
        );

        CatalogIntegrationResult result = catalogIntegrationApp.processProducts(List.of(listing));

        assertThat(result.productsInserted()).isZero();
        assertThat(result.sellerLinksCreated()).isEqualTo(1);

        try (Connection connection = databaseConfig.getConnection();
             Statement productStatement = connection.createStatement();
             Statement sellerProductStatement = connection.createStatement();
             ResultSet product = productStatement.executeQuery("""
                     SELECT Name, Brand, Category, Availability
                     FROM Product
                     """);
             ResultSet sellerProduct = sellerProductStatement.executeQuery("""
                     SELECT SellerProductName, SellerBrand, SellerCategory
                     FROM SellerProduct
                     """)) {
            assertThat(product.next()).isTrue();
            assertThat(product.getString("Name")).isEqualTo("Smartphone Galaxy S23");
            assertThat(product.getString("Brand")).isEqualTo("Samsung");
            assertThat(product.getString("Category")).isEqualTo("Electronics");
            assertThat(product.getString("Availability")).isEqualTo(Availability.AVAILABLE.name());
            assertThat(product.next()).isFalse();

            assertThat(sellerProduct.next()).isTrue();
            assertThat(sellerProduct.getString("SellerProductName")).isEqualTo("Smartphone  Galaxy S23");
            assertThat(sellerProduct.getString("SellerBrand")).isEqualTo("SAMSUNG");
            assertThat(sellerProduct.getString("SellerCategory")).isEqualTo("Phones");
            assertThat(sellerProduct.next()).isFalse();
        }
    }

    @Test
    void shouldTreatEmptyFeedAsNoOp() {
        CatalogIntegrationResult result = catalogIntegrationApp.processProducts(List.of());

        assertThat(result.totalProcessed()).isZero();
        assertThat(result.productsInserted()).isZero();
        assertThat(result.sellerLinksCreated()).isZero();
        assertThat(result.sellerLinksSkipped()).isZero();
        assertThat(result.itemsFailed()).isZero();
    }

    @Test
    void shouldReuseSellerAcrossCasingAndAllowMultipleListingIds() throws Exception {
        SellerProduct first = createSellerProduct(
                "list-1", "MegaStore", "Brand New Product", "Acme", "Gadgets"
        );
        SellerProduct second = createSellerProduct(
                "list-2", " megastore ", "Brand New Product", "Acme", "Gadgets"
        );

        catalogIntegrationApp.processProducts(List.of(first, second));

        try (Connection connection = databaseConfig.getConnection();
             Statement sellersStatement = connection.createStatement();
             Statement productsStatement = connection.createStatement();
             Statement linksStatement = connection.createStatement();
             ResultSet sellers = sellersStatement.executeQuery("SELECT COUNT(*) AS total FROM Seller");
             ResultSet products = productsStatement.executeQuery("SELECT COUNT(*) AS total FROM Product");
             ResultSet links = linksStatement.executeQuery("SELECT COUNT(*) AS total FROM SellerProduct")) {
            sellers.next();
            products.next();
            links.next();
            assertThat(sellers.getInt("total")).isEqualTo(1);
            assertThat(products.getInt("total")).isEqualTo(2);
            assertThat(links.getInt("total")).isEqualTo(2);
        }
    }

    // Anticipates unknown future feeds: null brands normalize to empty and can merge distinct listings by name.
    @Test
    void shouldMergeListingsWithNullBrandWhenNormalizedNamesMatch() throws Exception {
        catalogIntegrationApp.processProducts(List.of(
                createSellerProduct("null-brand-a", "StoreA", "Generic Widget", null, "Tools"),
                createSellerProduct("null-brand-b", "StoreB", "Generic  Widget", null, "Hardware")
        ));

        try (Connection connection = databaseConfig.getConnection();
             Statement productsStatement = connection.createStatement();
             Statement linksStatement = connection.createStatement();
             ResultSet products = productsStatement.executeQuery("""
                     SELECT COUNT(*) AS total
                     FROM Product
                     WHERE NormalizedProductName = 'generic widget' AND NormalizedBrand = ''
                     """);
             ResultSet links = linksStatement.executeQuery(
                     "SELECT COUNT(*) AS total FROM SellerProduct")) {
            products.next();
            links.next();
            assertThat(products.getInt("total")).isEqualTo(1);
            assertThat(links.getInt("total")).isEqualTo(2);
        }
    }

    // Anticipates unknown future feeds: re-sending the same seller listing does not refresh snapshot columns today.
    @Test
    void shouldKeepOriginalSellerSnapshotWhenSameListingIsResentWithChangedFields() throws Exception {
        catalogIntegrationApp.processProducts(List.of(
                createSellerProduct("listing-1", "MegaStore", "Original Name", "Acme", "Gadgets")
        ));
        CatalogIntegrationResult secondRun = catalogIntegrationApp.processProducts(List.of(
                createSellerProduct("listing-1", "MegaStore", "Updated Name", "Acme", "Accessories")
        ));

        assertThat(secondRun.sellerLinksCreated()).isZero();
        assertThat(secondRun.sellerLinksSkipped()).isEqualTo(1);

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet snapshot = statement.executeQuery("""
                     SELECT SellerProductName, SellerCategory
                     FROM SellerProduct
                     WHERE SellerProductId = 'listing-1'
                     """)) {
            snapshot.next();
            assertThat(snapshot.getString("SellerProductName")).isEqualTo("Original Name");
            assertThat(snapshot.getString("SellerCategory")).isEqualTo("Gadgets");
            assertThat(snapshot.next()).isFalse();
        }
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

        CatalogIntegrationResult result = app.startApp(input);

        assertThat(result.totalProcessed()).isEqualTo(2);
        assertThat(result.itemsFailed()).isEqualTo(1);
        assertThat(result.productsInserted()).isEqualTo(1);
        assertThat(result.sellerLinksCreated()).isEqualTo(1);
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).errorMessage()).isEqualTo("simulated persistence failure");

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
            try (ResultSet sellers = statement.executeQuery(
                    "SELECT COUNT(*) AS total FROM Seller WHERE NormalizedName = 'STOREA'")) {
                sellers.next();
                // Seller may already exist from the failed item before product persistence failed.
                assertThat(sellers.getInt("total")).isEqualTo(1);
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
