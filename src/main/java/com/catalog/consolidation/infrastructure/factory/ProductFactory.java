package com.catalog.consolidation.infrastructure.factory;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.model.Product;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductFactory {

    public Product create(ResultSet resultSet) throws SQLException {
        Product product = new Product();
        product.setId(resultSet.getLong("Id"));
        product.setName(resultSet.getString("Name"));
        product.setBrand(resultSet.getString("Brand"));
        product.setCategory(resultSet.getString("Category"));
        product.setNormalizedProductName(resultSet.getString("NormalizedProductName"));
        product.setNormalizedBrand(resultSet.getString("NormalizedBrand"));
        product.setAvailability(Availability.valueOf(resultSet.getString("Availability")));
        return product;
    }
}
