package com.catalog.consolidation.infrastructure.factory;

import com.catalog.consolidation.domain.model.Seller;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SellerFactory {

    public Seller create(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            throw new IllegalStateException("Seller not found in result set");
        }

        Seller seller = new Seller();
        seller.setId(resultSet.getLong("Id"));
        seller.setName(resultSet.getString("Name"));
        seller.setNormalizedName(resultSet.getString("NormalizedName"));
        return seller;
    }
}
