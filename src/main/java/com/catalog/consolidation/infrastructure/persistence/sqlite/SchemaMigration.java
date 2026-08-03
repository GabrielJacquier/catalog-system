package com.catalog.consolidation.infrastructure.persistence.sqlite;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.service.ProductNormalizationService;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SchemaMigration {

    private final DatabaseConfig databaseConfig;
    private final ProductNormalizationService productNormalizationService;

    public SchemaMigration(DatabaseConfig databaseConfig, ProductNormalizationService productNormalizationService) {
        this.databaseConfig = databaseConfig;
        this.productNormalizationService = productNormalizationService;
    }

    public void run() throws SQLException {
        try (Connection connection = databaseConfig.getConnection()) {
            if (isMigrationApplied(connection)) {
                return;
            }

            connection.setAutoCommit(false);
            try {
                applyMigration(connection);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private boolean isMigrationApplied(Connection connection) throws SQLException {
        return columnExists(connection, "Product", "Availability");
    }

    private void applyMigration(Connection connection) throws SQLException {
        applySchemaMigrations(connection);
        applyBackfills(connection);
    }

    private void applySchemaMigrations(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "Product", "Availability", "TEXT DEFAULT 'AVAILABLE'");
        addColumnIfMissing(connection, "Product", "NormalizedProductName", "TEXT");
        addColumnIfMissing(connection, "Product", "NormalizedBrand", "TEXT");

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_product_normalized
                    ON Product(NormalizedProductName, NormalizedBrand)
                    """);
        }

        createSellerTable(connection);
        recreateSellerProductTable(connection);
    }

    private void applyBackfills(Connection connection) throws SQLException {
        List<ProductRow> rows = loadAllProducts(connection);

        String updateSql = """
                UPDATE Product
                SET Availability = ?, NormalizedProductName = ?, NormalizedBrand = ?
                WHERE Id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            for (ProductRow row : rows) {
                statement.setString(1, Availability.AVAILABLE.name());
                statement.setString(2, productNormalizationService.normalizeProductName(row.name()));
                statement.setString(3, productNormalizationService.normalizeBrand(row.brand()));
                statement.setLong(4, row.id());
                statement.executeUpdate();
            }
        }
    }

    private List<ProductRow> loadAllProducts(Connection connection) throws SQLException {
        String selectSql = "SELECT Id, Name, Brand FROM Product";
        List<ProductRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(selectSql)) {
            while (resultSet.next()) {
                rows.add(new ProductRow(
                        resultSet.getLong("Id"),
                        resultSet.getString("Name"),
                        resultSet.getString("Brand")
                ));
            }
        }
        return rows;
    }

    private void addColumnIfMissing(Connection connection, String table, String column, String definition)
            throws SQLException {
        if (!columnExists(connection, table, column)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        String sql = "PRAGMA table_info(" + table + ")";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private void createSellerTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Seller (
                        Id INTEGER PRIMARY KEY AUTOINCREMENT,
                        Name TEXT NOT NULL,
                        NormalizedName TEXT NOT NULL,
                        UNIQUE (NormalizedName)
                    )
                    """);
        }
    }

    private void recreateSellerProductTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS SellerProduct");
            statement.execute("""
                    CREATE TABLE SellerProduct (
                        Id INTEGER PRIMARY KEY AUTOINCREMENT,
                        SellerId INTEGER NOT NULL REFERENCES Seller(Id),
                        ProductId INTEGER NOT NULL REFERENCES Product(Id),
                        SellerProductId TEXT NOT NULL,
                        SellerProductName TEXT NOT NULL,
                        SellerBrand TEXT,
                        SellerCategory TEXT,
                        UNIQUE (SellerId, SellerProductId)
                    )
                    """);
        }
    }

    private record ProductRow(long id, String name, String brand) {
    }
}
