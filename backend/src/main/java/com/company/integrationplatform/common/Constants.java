package com.company.integrationplatform.common;

public final class Constants {

    private Constants() {}

    // Roles
    public static final String ROLE_ADMIN    = "ROLE_ADMIN";
    public static final String ROLE_ANALYST  = "ROLE_ANALYST";
    public static final String ROLE_OPERATOR = "ROLE_OPERATOR";

    // JWT
    public static final String BEARER_PREFIX      = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    // Pagination
    public static final int DEFAULT_PAGE      = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;

    // Audit actions
    public static final String ACTION_LOGIN               = "USER_LOGIN";
    public static final String ACTION_REGISTER            = "USER_REGISTER";
    public static final String ACTION_LOGOUT              = "USER_LOGOUT";
    public static final String ACTION_CREATE_SOURCE       = "CREATE_DATA_SOURCE";
    public static final String ACTION_UPDATE_SOURCE       = "UPDATE_DATA_SOURCE";
    public static final String ACTION_DELETE_SOURCE       = "DELETE_DATA_SOURCE";
    public static final String ACTION_INGEST_CSV          = "INGEST_CSV";
    public static final String ACTION_INGEST_API          = "INGEST_API";
    public static final String ACTION_TRANSFORM           = "TRANSFORM_DATA";
    public static final String ACTION_SYNC                = "SYNC_JOB";
    public static final String ACTION_CREATE_USER         = "CREATE_USER";
    public static final String ACTION_UPDATE_USER         = "UPDATE_USER";
    public static final String ACTION_DELETE_USER         = "DELETE_USER";

    // CSV ingestion audit actions
    public static final String ACTION_UPLOAD_CSV          = "UPLOAD_CSV";
    public static final String ACTION_INGESTION_STARTED   = "INGESTION_STARTED";
    public static final String ACTION_INGESTION_COMPLETED = "INGESTION_COMPLETED";
    public static final String ACTION_INGESTION_FAILED    = "INGESTION_FAILED";

    // Transformation rule audit actions
    public static final String ACTION_CREATE_RULE             = "CREATE_RULE";
    public static final String ACTION_UPDATE_RULE             = "UPDATE_RULE";
    public static final String ACTION_DELETE_RULE             = "DELETE_RULE";
    public static final String ACTION_TRANSFORMATION_EXECUTED = "TRANSFORMATION_EXECUTED";

    // Synchronization audit actions
    public static final String ACTION_SYNC_STARTED   = "SYNC_STARTED";
    public static final String ACTION_SYNC_COMPLETED = "SYNC_COMPLETED";
    public static final String ACTION_SYNC_FAILED    = "SYNC_FAILED";

    // Circuit breaker audit actions
    public static final String ACTION_CIRCUIT_OPENED    = "CIRCUIT_BREAKER_OPENED";
    public static final String ACTION_CIRCUIT_HALF_OPEN = "CIRCUIT_BREAKER_HALF_OPEN";
    public static final String ACTION_CIRCUIT_RECOVERED = "CIRCUIT_BREAKER_RECOVERED";
    public static final String ACTION_CIRCUIT_RESET     = "CIRCUIT_BREAKER_RESET";

    // CSV validation
    public static final long   MAX_CSV_FILE_SIZE_BYTES    = 10L * 1024 * 1024; // 10 MB
    public static final String CSV_CONTENT_TYPE           = "text/csv";
    public static final String CSV_EXTENSION              = ".csv";
}
