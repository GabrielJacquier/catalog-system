package com.catalog.consolidation.application.service;

import com.catalog.consolidation.application.dto.SellerProductInput;
import com.catalog.consolidation.application.mapper.ProductMapper;
import com.catalog.consolidation.domain.model.ImportCatalogResult;
import com.catalog.consolidation.domain.model.SellerStatus;
import com.catalog.consolidation.domain.service.ProductMatcher;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;
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

class ImportCatalogServiceIT {

    @TempDir
    Path tempDir;

    private DatabaseConfig databaseConfig;
    private ImportCatalogService importCatalogService;

    @BeforeEach
    void setUp() throws Exception {
        Path databasePath = tempDir.resolve("catalog.db");
        createSeedDatabase(databasePath);
        databaseConfig = new DatabaseConfig(databasePath.toString());
        new SchemaMigration(databaseConfig, new ProductMatcher()).run();

        importCatalogService = new ImportCatalogService(
                new SqliteProductRepository(databaseConfig),
                new SqliteSellerProductRepository(databaseConfig),
                new ProductMapper(),
                new ProductMatcher()
        );
    }

    @Test
    void shouldReuseActiveProductWithoutChangingStatus() throws Exception {
        SellerProductInput input = createInput(
                "dup-1", "MegaStore", "Smartphone  Galaxy S23", "Samsung", "Electronics"
        );

        ImportCatalogResult result = importCatalogService.execute(List.of(input));

        assertThat(result.productsInserted()).isZero();
        assertThat(result.sellerLinksCreated()).isEqualTo(1);

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet products = statement.executeQuery("SELECT COUNT(*) AS total FROM Product");
             ResultSet sellerProducts = statement.executeQuery("SELECT COUNT(*) AS total FROM SellerProduct")) {
            products.next();
            sellerProducts.next();
            assertThat(products.getInt("total")).isEqualTo(1);
            assertThat(sellerProducts.getInt("total")).isEqualTo(1);
        }

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet product = statement.executeQuery(
                     "SELECT SellerStatus FROM Product WHERE Name = 'Smartphone Galaxy S23'")) {
            product.next();
            assertThat(product.getString("SellerStatus")).isEqualTo(SellerStatus.ACTIVE_TO_SELLER.name());
        }
    }

    @Test
    void shouldInsertNewProductAsInactive() throws Exception {
        SellerProductInput input = createInput(
                "new-1", "MegaStore", "Brand New Product", "Acme", "Gadgets"
        );

        ImportCatalogResult result = importCatalogService.execute(List.of(input));

        assertThat(result.productsInserted()).isEqualTo(1);

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet product = statement.executeQuery(
                     "SELECT SellerStatus FROM Product WHERE Name = 'Brand New Product'")) {
            product.next();
            assertThat(product.getString("SellerStatus")).isEqualTo(SellerStatus.INACTIVE_TO_SELLER.name());
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

        importCatalogService.execute(List.of(first, second));

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet products = statement.executeQuery("SELECT COUNT(*) AS total FROM Product");
             ResultSet sellerProducts = statement.executeQuery("SELECT COUNT(*) AS total FROM SellerProduct")) {
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

        importCatalogService.execute(List.of(input));
        ImportCatalogResult secondRun = importCatalogService.execute(List.of(input));

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
        SellerProductInput input = new SellerProductInput();
        input.setId(id);
        input.setSellerName(sellerName);
        input.setName(name);
        input.setBrand(brand);
        input.setCategory(category);
        return input;
    }
}
