package com.catalog.consolidation.infrastructure.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private final String databasePath;

    public DatabaseConfig(String databasePath) {
        this.databasePath = databasePath;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }
}
