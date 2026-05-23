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
    public static final String ACTION_LOGIN           = "USER_LOGIN";
    public static final String ACTION_REGISTER        = "USER_REGISTER";
    public static final String ACTION_LOGOUT          = "USER_LOGOUT";
    public static final String ACTION_CREATE_SOURCE   = "CREATE_DATA_SOURCE";
    public static final String ACTION_UPDATE_SOURCE   = "UPDATE_DATA_SOURCE";
    public static final String ACTION_DELETE_SOURCE   = "DELETE_DATA_SOURCE";
    public static final String ACTION_INGEST_CSV      = "INGEST_CSV";
    public static final String ACTION_INGEST_API      = "INGEST_API";
    public static final String ACTION_TRANSFORM       = "TRANSFORM_DATA";
    public static final String ACTION_SYNC            = "SYNC_JOB";
    public static final String ACTION_CREATE_USER     = "CREATE_USER";
    public static final String ACTION_UPDATE_USER     = "UPDATE_USER";
    public static final String ACTION_DELETE_USER     = "DELETE_USER";
}
