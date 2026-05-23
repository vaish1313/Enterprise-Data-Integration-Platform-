package com.company.integrationplatform.datasource;

import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "data_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSourceEntity extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "connection_details", columnDefinition = "jsonb")
    private Map<String, String> connectionDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SourceStatus status = SourceStatus.INACTIVE;

    @Column(length = 500)
    private String description;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public enum SourceType {
        CSV, REST_API, DATABASE
    }

    public enum SourceStatus {
        ACTIVE, INACTIVE, ERROR
    }
}
