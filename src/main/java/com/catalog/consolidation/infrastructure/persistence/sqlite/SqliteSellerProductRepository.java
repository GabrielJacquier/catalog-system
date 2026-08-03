package com.catalog.consolidation.infrastructure.persistence.sqlite;

import com.catalog.consolidation.domain.model.SellerProductLink;
import com.catalog.consolidation.domain.ports.out.SellerProductRepository;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqliteSellerProductRepository implements SellerProductRepository {

    private final DatabaseConfig databaseConfig;

    public SqliteSellerProductRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @Override
    public boolean link(long productId, SellerProductLink sellerProductLink) {
        String sql = """
                INSERT OR IGNORE INTO SellerProduct (
                    SellerName, ProductId, SellerProductId,
                    SellerProductName, SellerBrand, SellerCategory
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sellerProductLink.getSellerName());
            statement.setLong(2, productId);
            statement.setString(3, sellerProductLink.getSellerProductId());
            statement.setString(4, sellerProductLink.getSellerProductName());
            statement.setString(5, sellerProductLink.getSellerBrand());
            statement.setString(6, sellerProductLink.getSellerCategory());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to link seller product", ex);
        }
    }
}
