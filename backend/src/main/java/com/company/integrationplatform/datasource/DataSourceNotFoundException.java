package com.company.integrationplatform.datasource;

import java.util.UUID;

/**
 * Thrown when a requested DataSource cannot be found in the database.
 * Maps to HTTP 404 Not Found via GlobalExceptionHandler.
 */
public class DataSourceNotFoundException extends RuntimeException {

    public DataSourceNotFoundException(UUID id) {
        super("Data source not found with id: " + id);
    }

    public DataSourceNotFoundException(String name) {
        super("Data source not found with name: " + name);
    }
}
