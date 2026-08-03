package com.catalog.consolidation.application;

import com.catalog.consolidation.domain.model.Availability;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        ProductMatcher productMatcher = new ProductMatcher();
        SchemaMigration schemaMigration = new SchemaMigration(databaseConfig, productMatcher);
        schemaMigration.run();

        SellerProductPreparationService preparationService = new SellerProductPreparationService(productMatcher);
        ProductRepository productRepository = new SqliteProductRepository(databaseConfig);
        SellerProductRepository sellerProductRepository = new SqliteSellerProductRepository(databaseConfig);
        ProductInsertionService productInsertionService = new ProductInsertionService(
                preparationService,
                productRepository,
                sellerProductRepository
        );

        catalogIntegrationApp = new CatalogIntegrationApp(
                schemaMigration,
                new JsonCatalogReader(),
                productInsertionService
        );
    }

    @Test
    void shouldReuseActiveProductWithoutChangingStatus() throws Exception {
        SellerProductInput input = createInput(
                "dup-1", "MegaStore", "Smartphone  Galaxy S23", "Samsung", "Electronics"
        );

        CatalogIntegrationResult result = catalogIntegrationApp.processProducts(List.of(input));

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
        SellerProductInput input = createInput(
                "new-1", "MegaStore", "Brand New Product", "Acme", "Gadgets"
        );

        CatalogIntegrationResult result = catalogIntegrationApp.processProducts(List.of(input));

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
        SellerProductInput first = createInput(
                "seller-a", "StoreA", "Smartphone  Galaxy S23", "Samsung", "Electronics"
        );
        SellerProductInput second = createInput(
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
        SellerProductInput input = createInput(
                "idem-1", "MegaStore", "Brand New Product", "Acme", "Gadgets"
        );

        catalogIntegrationApp.processProducts(List.of(input));
        CatalogIntegrationResult secondRun = catalogIntegrationApp.processProducts(List.of(input));

        assertThat(secondRun.productsInserted()).isZero();
        assertThat(secondRun.sellerLinksCreated()).isZero();
        assertThat(secondRun.sellerLinksSkipped()).isEqualTo(1);
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

    private SellerProductInput createInput(String id, String sellerName, String name,
                                           String brand, String category) {
        return new SellerProductInput(id, sellerName, name, brand, category);
    }
}
