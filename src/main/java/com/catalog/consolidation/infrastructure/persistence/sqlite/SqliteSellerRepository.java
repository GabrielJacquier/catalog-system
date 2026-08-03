package com.catalog.consolidation.infrastructure.persistence.sqlite;

import com.catalog.consolidation.domain.model.Seller;
import com.catalog.consolidation.domain.repository.SellerRepository;
import com.catalog.consolidation.infrastructure.config.DatabaseConfig;
import com.catalog.consolidation.infrastructure.factory.SellerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqliteSellerRepository implements SellerRepository {

    private final DatabaseConfig databaseConfig;
    private final SellerFactory sellerFactory;

    public SqliteSellerRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        this.sellerFactory = new SellerFactory();
    }

    @Override
    public Seller insertIfNotExistsAndFetch(Seller seller) {
        try (Connection connection = databaseConfig.getConnection()) {
            insertSeller(connection, seller);
            return fetchByNormalizedName(connection, seller.getNormalizedName());
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to upsert seller", ex);
        }
    }

    private void insertSeller(Connection connection, Seller seller) throws SQLException {
        String sql = """
                INSERT INTO Seller (Name, NormalizedName)
                VALUES (?, ?)
                ON CONFLICT(NormalizedName) DO NOTHING
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, seller.getName());
            statement.setString(2, seller.getNormalizedName());
            statement.executeUpdate();
        }
    }

    private Seller fetchByNormalizedName(Connection connection, String normalizedName) throws SQLException {
        String sql = """
                SELECT Id, Name, NormalizedName
                FROM Seller
                WHERE NormalizedName = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return sellerFactory.create(resultSet);
            }
        }
    }
}
