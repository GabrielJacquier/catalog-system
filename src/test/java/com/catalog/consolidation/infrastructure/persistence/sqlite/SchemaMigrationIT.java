package com.catalog.consolidation.infrastructure.persistence.sqlite;

import com.catalog.consolidation.domain.service.ProductMatcher;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationIT {

    @TempDir
    Path tempDir;

    @Test
    void shouldApplyMigrationAndBackfillExistingProducts() throws Exception {
        Path databasePath = tempDir.resolve("catalog.db");
        createSeedDatabase(databasePath);

        DatabaseConfig databaseConfig = new DatabaseConfig(databasePath.toString());
        new SchemaMigration(databaseConfig, new ProductMatcher()).run();

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement()) {

            assertThat(columnExists(connection, "Product", "SellerStatus")).isTrue();
            assertThat(columnExists(connection, "Product", "NormalizedProductName")).isTrue();
            assertThat(columnExists(connection, "Product", "NormalizedBrand")).isTrue();

            try (ResultSet products = statement.executeQuery(
                    "SELECT SellerStatus, NormalizedProductName, NormalizedBrand FROM Product")) {
                assertThat(products.next()).isTrue();
                assertThat(products.getString("SellerStatus")).isEqualTo("ACTIVE_TO_SELLER");
                assertThat(products.getString("NormalizedProductName")).isEqualTo("smartphone galaxy s23");
                assertThat(products.getString("NormalizedBrand")).isEqualTo("samsung");
                assertThat(products.next()).isFalse();
            }

            try (ResultSet sellerProductColumns = statement.executeQuery("PRAGMA table_info(SellerProduct)")) {
                int columnCount = 0;
                while (sellerProductColumns.next()) {
                    columnCount++;
                }
                assertThat(columnCount).isEqualTo(7);
            }
        }
    }

    @Test
    void shouldBeIdempotentOnSecondRun() throws Exception {
        Path databasePath = tempDir.resolve("catalog-idempotent.db");
        createSeedDatabase(databasePath);

        DatabaseConfig databaseConfig = new DatabaseConfig(databasePath.toString());
        SchemaMigration migration = new SchemaMigration(databaseConfig, new ProductMatcher());
        migration.run();
        migration.run();

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet versions = statement.executeQuery("SELECT COUNT(*) AS total FROM schema_version")) {
            versions.next();
            assertThat(versions.getInt("total")).isEqualTo(1);
        }
    }

    private void createSeedDatabase(Path databasePath) throws Exception {
        DatabaseConfig databaseConfig = new DatabaseConfig(databasePath.toString());
        try (Connection connection = databaseConfig.getConnection();
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

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
