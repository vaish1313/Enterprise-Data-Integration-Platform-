package com.company.integrationplatform.transformation;

import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transformation_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformationRule extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "data_source_id")
    private UUID dataSourceId;   // null = global rule

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    /**
     * For FIELD_MAPPING: {"sourceField": "targetField", ...}
     * For NORMALIZE:     {"field": "fieldName", "type": "UPPERCASE|LOWERCASE|TRIM"}
     * For TYPE_CAST:     {"field": "fieldName", "targetType": "INTEGER|DOUBLE|BOOLEAN"}
     * For RENAME:        {"oldName": "newName"}
     * For DEFAULT_VALUE: {"field": "fieldName", "defaultValue": "value"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, String> config;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "execution_order")
    @Builder.Default
    private int executionOrder = 0;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public enum RuleType {
        FIELD_MAPPING, NORMALIZE, TYPE_CAST, RENAME, DEFAULT_VALUE, REMOVE_FIELD
    }
}
