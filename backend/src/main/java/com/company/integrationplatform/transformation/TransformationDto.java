package com.company.integrationplatform.transformation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class TransformationDto {

    @Getter
    @Setter
    public static class CreateRequest {

        @NotBlank(message = "Rule name is required")
        private String name;

        private UUID dataSourceId;

        @NotNull(message = "Rule type is required")
        private TransformationRule.RuleType ruleType;

        private Map<String, String> config;

        private int executionOrder;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        private String name;
        private Map<String, String> config;
        private Boolean enabled;
        private Integer executionOrder;
    }

    @Getter
    @Builder
    public static class Response {

        private UUID id;
        private String name;
        private UUID dataSourceId;
        private TransformationRule.RuleType ruleType;
        private Map<String, String> config;
        private boolean enabled;
        private int executionOrder;
        private String createdBy;
        private LocalDateTime createdAt;

        public static Response from(TransformationRule rule) {
            return Response.builder()
                    .id(rule.getId())
                    .name(rule.getName())
                    .dataSourceId(rule.getDataSourceId())
                    .ruleType(rule.getRuleType())
                    .config(rule.getConfig())
                    .enabled(rule.isEnabled())
                    .executionOrder(rule.getExecutionOrder())
                    .createdBy(rule.getCreatedBy())
                    .createdAt(rule.getCreatedAt())
                    .build();
        }
    }
}
