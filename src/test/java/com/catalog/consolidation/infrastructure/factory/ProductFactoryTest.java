package com.catalog.consolidation.infrastructure.factory;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductFactoryTest {

    @TempDir
    Path tempDir;

    private final ProductFactory productFactory = new ProductFactory();

    @Test
    void shouldCreateProductFromResultSet() throws Exception {
        Path databasePath = tempDir.resolve("product-factory-test.db");
        DatabaseConfig databaseConfig = new DatabaseConfig(databasePath.toString());

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE Product (
                        Id INTEGER PRIMARY KEY AUTOINCREMENT,
                        Name TEXT NOT NULL,
                        Brand TEXT,
                        Category TEXT,
                        NormalizedProductName TEXT,
                        NormalizedBrand TEXT,
                        NormalizedCategory TEXT,
                        Availability TEXT
                    )
                    """);
            statement.execute("""
                    INSERT INTO Product (Name, Brand, Category, NormalizedProductName, NormalizedBrand, NormalizedCategory, Availability)
                    VALUES ('Smartphone Galaxy S23', 'Samsung', 'Electronics', 'smartphone galaxy s23', 'samsung', 'electronics', 'AVAILABLE')
                    """);

            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM Product")) {
                Product product = productFactory.create(resultSet);

                assertThat(product.getId()).isPositive();
                assertThat(product.getName()).isEqualTo("Smartphone Galaxy S23");
                assertThat(product.getBrand()).isEqualTo("Samsung");
                assertThat(product.getCategory()).isEqualTo("Electronics");
                assertThat(product.getNormalizedProductName()).isEqualTo("smartphone galaxy s23");
                assertThat(product.getNormalizedBrand()).isEqualTo("samsung");
                assertThat(product.getNormalizedCategory()).isEqualTo("electronics");
                assertThat(product.getAvailability()).isEqualTo(Availability.AVAILABLE);
            }
        }
    }

    @Test
    void shouldThrowWhenResultSetIsEmpty() throws Exception {
        Path databasePath = tempDir.resolve("product-factory-empty-test.db");
        DatabaseConfig databaseConfig = new DatabaseConfig(databasePath.toString());

        try (Connection connection = databaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE Product (
                        Id INTEGER PRIMARY KEY AUTOINCREMENT,
                        Name TEXT NOT NULL,
                        Brand TEXT,
                        Category TEXT,
                        NormalizedProductName TEXT,
                        NormalizedBrand TEXT,
                        NormalizedCategory TEXT,
                        Availability TEXT
                    )
                    """);

            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM Product")) {
                assertThatThrownBy(() -> productFactory.create(resultSet))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Product not found");
            }
        }
    }
}
