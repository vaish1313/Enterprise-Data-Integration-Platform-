package com.company.integrationplatform.transformation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO container for the Transformation Rules Engine module.
 */
public class TransformationDto {

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE REQUEST
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @Schema(name = "TransformationRuleCreateRequest",
            description = "Payload for creating a new transformation rule")
    public static class CreateRequest {

        @NotBlank(message = "Rule name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @Schema(description = "Unique rule name within the data source scope",
                example = "Normalize Customer Name", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Size(max = 500)
        @Schema(description = "Optional description of what this rule does",
                example = "Trims whitespace from the customer_name field")
        private String description;

        @Schema(description = "Data source UUID this rule applies to. Null = global rule.",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        private UUID dataSourceId;

        @NotNull(message = "Transformation type is required")
        @Schema(description = "Type of transformation to apply",
                example = "TRIM",
                allowableValues = {"DIRECT_MAPPING","UPPERCASE","LOWERCASE","TRIM",
                                   "CONCAT","DEFAULT_VALUE","DATE_FORMAT"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        private TransformationRule.TransformationType transformationType;

        @Size(max = 255, message = "Source field must not exceed 255 characters")
        @Schema(description = "Field name in the source record to read from",
                example = "customer_name")
        private String sourceField;

        @Size(max = 255, message = "Target field must not exceed 255 characters")
        @Schema(description = "Field name in the output record to write to. "
                            + "If null, result overwrites sourceField.",
                example = "customerName")
        private String targetField;

        @Size(max = 500)
        @Schema(description = """
                Multi-purpose field depending on transformationType:
                - DEFAULT_VALUE: the fallback value when sourceField is null/blank
                - DATE_FORMAT: the output date pattern (e.g. "yyyy-MM-dd")
                - CONCAT: the separator string between the two fields (e.g. " ")
                """,
                example = "Unknown")
        private String defaultValue;

        @Size(max = 500)
        @Schema(description = """
                Multi-purpose secondary config field:
                - CONCAT: the second field name to concatenate with sourceField
                - DATE_FORMAT: the input date pattern (e.g. "dd/MM/yyyy")
                """,
                example = "last_name")
        private String extraConfig;

        @Min(value = 0, message = "Execution order must be >= 0")
        @Schema(description = "Execution order — lower numbers run first",
                example = "10")
        private int executionOrder;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE REQUEST
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @Schema(name = "TransformationRuleUpdateRequest",
            description = "Payload for updating a transformation rule. All fields optional.")
    public static class UpdateRequest {

        @Size(max = 255)
        @Schema(description = "New rule name", example = "Normalize Customer Name v2")
        private String name;

        @Size(max = 500)
        @Schema(description = "Updated description")
        private String description;

        @Schema(description = "Updated transformation type",
                allowableValues = {"DIRECT_MAPPING","UPPERCASE","LOWERCASE","TRIM",
                                   "CONCAT","DEFAULT_VALUE","DATE_FORMAT"})
        private TransformationRule.TransformationType transformationType;

        @Size(max = 255)
        @Schema(description = "Updated source field", example = "cust_name")
        private String sourceField;

        @Size(max = 255)
        @Schema(description = "Updated target field", example = "customerName")
        private String targetField;

        @Size(max = 500)
        @Schema(description = "Updated default value / output pattern / separator")
        private String defaultValue;

        @Size(max = 500)
        @Schema(description = "Updated extra config (concat field / input date pattern)")
        private String extraConfig;

        @Schema(description = "Enable or disable this rule", example = "true")
        private Boolean active;

        @Min(0)
        @Schema(description = "Updated execution order", example = "20")
        private Integer executionOrder;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESPONSE
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "TransformationRuleResponse",
            description = "Transformation rule details returned by the API")
    public static class Response {

        @Schema(description = "Rule UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        private UUID id;

        @Schema(description = "Rule name", example = "Normalize Customer Name")
        private String name;

        @Schema(description = "Rule description")
        private String description;

        @Schema(description = "Scoped data source UUID (null = global)",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        private UUID dataSourceId;

        @Schema(description = "Transformation type", example = "TRIM")
        private TransformationRule.TransformationType transformationType;

        @Schema(description = "Source field name", example = "customer_name")
        private String sourceField;

        @Schema(description = "Target field name", example = "customerName")
        private String targetField;

        @Schema(description = "Default value / output pattern / separator", example = "Unknown")
        private String defaultValue;

        @Schema(description = "Extra config (concat field / input date pattern)", example = "last_name")
        private String extraConfig;

        @Schema(description = "Whether this rule is active", example = "true")
        private boolean active;

        @Schema(description = "Execution order", example = "10")
        private int executionOrder;

        @Schema(description = "Creator username", example = "analyst_user")
        private String createdBy;

        @Schema(description = "Creation timestamp", example = "2026-05-23T10:15:30")
        private LocalDateTime createdAt;

        @Schema(description = "Last update timestamp", example = "2026-05-23T12:00:00")
        private LocalDateTime updatedAt;

        public static Response from(TransformationRule rule) {
            return Response.builder()
                    .id(rule.getId())
                    .name(rule.getName())
                    .description(rule.getDescription())
                    .dataSourceId(rule.getDataSourceId())
                    .transformationType(rule.getTransformationType())
                    .sourceField(rule.getSourceField())
                    .targetField(rule.getTargetField())
                    .defaultValue(rule.getDefaultValue())
                    .extraConfig(rule.getExtraConfig())
                    .active(rule.isActive())
                    .executionOrder(rule.getExecutionOrder())
                    .createdBy(rule.getCreatedBy())
                    .createdAt(rule.getCreatedAt())
                    .updatedAt(rule.getUpdatedAt())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPLY RESULT
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "TransformationApplyResult",
            description = "Result of applying transformation rules to an ingestion job")
    public static class ApplyResult {

        @Schema(description = "Ingestion job ID the rules were applied to",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        private UUID jobId;

        @Schema(description = "Total records processed", example = "1500")
        private int totalRecords;

        @Schema(description = "Records successfully transformed", example = "1498")
        private int transformedRecords;

        @Schema(description = "Records that failed transformation", example = "2")
        private int failedRecords;

        @Schema(description = "Number of active rules applied", example = "5")
        private int rulesApplied;

        @Schema(description = "Per-rule execution summary")
        private List<RuleSummary> ruleSummaries;

        @Schema(description = "Transformation start time")
        private LocalDateTime startedAt;

        @Schema(description = "Transformation completion time")
        private LocalDateTime completedAt;
    }

    @Getter
    @Builder
    @Schema(name = "RuleSummary", description = "Execution summary for a single transformation rule")
    public static class RuleSummary {

        @Schema(description = "Rule UUID")
        private UUID ruleId;

        @Schema(description = "Rule name", example = "Normalize Customer Name")
        private String ruleName;

        @Schema(description = "Transformation type", example = "TRIM")
        private TransformationRule.TransformationType transformationType;

        @Schema(description = "Number of records this rule was applied to", example = "1498")
        private int appliedCount;

        @Schema(description = "Number of records this rule skipped (field absent)", example = "2")
        private int skippedCount;

        @Schema(description = "Number of records this rule failed on", example = "0")
        private int errorCount;
    }
}
