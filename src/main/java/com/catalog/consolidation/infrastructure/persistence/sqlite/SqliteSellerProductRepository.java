package com.catalog.consolidation.infrastructure.persistence.sqlite;

import com.catalog.consolidation.domain.model.ProductLinkedToSeller;
import com.catalog.consolidation.domain.repository.SellerProductRepository;
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
    public boolean link(ProductLinkedToSeller productLinkedToSeller) {
        String sql = """
                INSERT OR IGNORE INTO SellerProduct (
                    SellerId, ProductId, SellerProductId,
                    SellerProductName, SellerBrand, SellerCategory
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productLinkedToSeller.seller().getId());
            statement.setLong(2, productLinkedToSeller.product().getId());
            statement.setString(3, productLinkedToSeller.sellerProductId());
            statement.setString(4, productLinkedToSeller.sellerProductName());
            statement.setString(5, productLinkedToSeller.sellerBrand());
            statement.setString(6, productLinkedToSeller.sellerCategory());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to link seller product", ex);
        }
    }
}
