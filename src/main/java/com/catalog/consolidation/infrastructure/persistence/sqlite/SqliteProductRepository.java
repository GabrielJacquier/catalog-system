package com.catalog.consolidation.infrastructure.persistence.sqlite;

import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductUpsertResult;
import com.catalog.consolidation.domain.model.SellerStatus;
import com.catalog.consolidation.domain.ports.out.ProductRepository;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqliteProductRepository implements ProductRepository {

    private final DatabaseConfig databaseConfig;

    public SqliteProductRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @Override
    public ProductUpsertResult insertIfNotExistsAndFetch(Product product) {
        try (Connection connection = databaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int insertedRows = insertProduct(connection, product);
                Product persisted = fetchByNormalizedKeys(
                        connection,
                        product.getNormalizedProductName(),
                        product.getNormalizedBrand()
                );
                connection.commit();
                return new ProductUpsertResult(persisted, insertedRows > 0);
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to upsert product", ex);
        }
    }

    private int insertProduct(Connection connection, Product product) throws SQLException {
        String sql = """
                INSERT INTO Product (Name, Brand, Category, NormalizedProductName, NormalizedBrand, SellerStatus)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(NormalizedProductName, NormalizedBrand) DO NOTHING
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getName());
            statement.setString(2, product.getBrand());
            statement.setString(3, product.getCategory());
            statement.setString(4, product.getNormalizedProductName());
            statement.setString(5, product.getNormalizedBrand());
            statement.setString(6, product.getSellerStatus().name());
            return statement.executeUpdate();
        }
    }

    private Product fetchByNormalizedKeys(Connection connection,
                                          String normalizedProductName,
                                          String normalizedBrand) throws SQLException {
        String sql = """
                SELECT Id, Name, Brand, Category, NormalizedProductName, NormalizedBrand, SellerStatus
                FROM Product
                WHERE NormalizedProductName = ? AND NormalizedBrand = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedProductName);
            statement.setString(2, normalizedBrand);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Product not found after upsert");
                }
                return mapProduct(resultSet);
            }
        }
    }

    static Product mapProduct(ResultSet resultSet) throws SQLException {
        Product product = new Product();
        product.setId(resultSet.getLong("Id"));
        product.setName(resultSet.getString("Name"));
        product.setBrand(resultSet.getString("Brand"));
        product.setCategory(resultSet.getString("Category"));
        product.setNormalizedProductName(resultSet.getString("NormalizedProductName"));
        product.setNormalizedBrand(resultSet.getString("NormalizedBrand"));
        product.setSellerStatus(SellerStatus.valueOf(resultSet.getString("SellerStatus")));
        return product;
    }
}
