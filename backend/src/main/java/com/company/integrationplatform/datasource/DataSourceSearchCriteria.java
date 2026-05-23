package com.company.integrationplatform.datasource;

import lombok.Builder;
import lombok.Getter;

/**
 * Encapsulates all optional search/filter parameters for data source queries.
 * Passed from the controller to the service and used to build a JPA Specification.
 */
@Getter
@Builder
public class DataSourceSearchCriteria {

    /** Partial, case-insensitive match on the data source name. */
    private String name;

    /** Exact match on source type: CSV, REST_API, DATABASE. */
    private DataSourceEntity.SourceType sourceType;

    /** Exact match on status: ACTIVE, INACTIVE, ERROR. */
    private DataSourceEntity.SourceStatus status;
}
