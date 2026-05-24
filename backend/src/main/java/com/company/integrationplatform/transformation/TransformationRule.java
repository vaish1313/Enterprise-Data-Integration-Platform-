package com.company.integrationplatform.transformation;

import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a single transformation rule applied to ingestion records.
 *
 * <p>Rules are applied in {@code executionOrder} (ascending) during ingestion
 * and when explicitly triggered via the apply endpoint.
 *
 * <p><b>Transformation types:</b>
 * <ul>
 *   <li>{@code DIRECT_MAPPING}  — copy sourceField value to targetField as-is</li>
 *   <li>{@code UPPERCASE}       — convert sourceField value to upper case, write to targetField</li>
 *   <li>{@code LOWERCASE}       — convert sourceField value to lower case, write to targetField</li>
 *   <li>{@code TRIM}            — strip leading/trailing whitespace, write to targetField</li>
 *   <li>{@code CONCAT}          — concatenate sourceField + concatField (separator from config), write to targetField</li>
 *   <li>{@code DEFAULT_VALUE}   — if sourceField is null/blank, write defaultValue to targetField</li>
 *   <li>{@code DATE_FORMAT}     — parse sourceField with sourcePattern, format to targetPattern, write to targetField</li>
 * </ul>
 */
@Entity
@Table(name = "transformation_rules",
        indexes = {
                @Index(name = "idx_transform_datasource", columnList = "data_source_id"),
                @Index(name = "idx_transform_active",     columnList = "active"),
                @Index(name = "idx_transform_order",      columnList = "execution_order")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformationRule extends BaseEntity {

    // ── Identity ──────────────────────────────────────────────────────────────

    /** Human-readable rule name. Must be unique within a data source scope. */
    @Column(nullable = false, length = 255)
    private String name;

    /** Optional description explaining the rule's purpose. */
    @Column(length = 500)
    private String description;

    // ── Scope ─────────────────────────────────────────────────────────────────

    /**
     * Data source this rule applies to.
     * {@code null} means the rule is global and applies to all data sources.
     */
    @Column(name = "data_source_id")
    private UUID dataSourceId;

    // ── Transformation definition ─────────────────────────────────────────────

    /** The type of transformation to apply. */
    @Enumerated(EnumType.STRING)
    @Column(name = "transformation_type", nullable = false, length = 50)
    private TransformationType transformationType;

    /**
     * The field name in the source record to read from.
     * Required for all types except DEFAULT_VALUE (where it is the field to check).
     */
    @Column(name = "source_field", length = 255)
    private String sourceField;

    /**
     * The field name in the output record to write to.
     * If null, the result overwrites sourceField in-place.
     */
    @Column(name = "target_field", length = 255)
    private String targetField;

    /**
     * Used by DEFAULT_VALUE: written to targetField when sourceField is null/blank.
     * Used by DATE_FORMAT: the output date pattern (e.g. "yyyy-MM-dd").
     * Used by CONCAT: the separator string between the two fields.
     */
    @Column(name = "default_value", length = 500)
    private String defaultValue;

    /**
     * Used by CONCAT: the second field to concatenate with sourceField.
     * Used by DATE_FORMAT: the input date pattern (e.g. "dd/MM/yyyy").
     */
    @Column(name = "extra_config", length = 500)
    private String extraConfig;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Whether this rule is active. Inactive rules are skipped during transformation. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Execution order — lower numbers run first. Ties are broken by creation time. */
    @Column(name = "execution_order", nullable = false)
    @Builder.Default
    private int executionOrder = 0;

    /** Username of the user who created this rule. */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    // ── Enum ──────────────────────────────────────────────────────────────────

    public enum TransformationType {
        /** Copy sourceField value to targetField unchanged. */
        DIRECT_MAPPING,

        /** Convert sourceField value to UPPER CASE and write to targetField. */
        UPPERCASE,

        /** Convert sourceField value to lower case and write to targetField. */
        LOWERCASE,

        /** Strip leading/trailing whitespace from sourceField and write to targetField. */
        TRIM,

        /**
         * Concatenate sourceField and extraConfig (second field name) with defaultValue
         * as separator, write result to targetField.
         * Example: sourceField="firstName", extraConfig="lastName", defaultValue=" "
         * → targetField = "John Doe"
         */
        CONCAT,

        /**
         * If sourceField is null or blank, write defaultValue to targetField.
         * Otherwise copy sourceField value to targetField unchanged.
         */
        DEFAULT_VALUE,

        /**
         * Parse sourceField using extraConfig as the input date pattern,
         * reformat using defaultValue as the output pattern, write to targetField.
         * Example: extraConfig="dd/MM/yyyy", defaultValue="yyyy-MM-dd"
         */
        DATE_FORMAT
    }
}
