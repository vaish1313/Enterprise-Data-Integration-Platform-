package com.company.integrationplatform.transformation;

/**
 * Thrown when a transformation rule fails to execute against a specific record.
 * Contains the rule name and type for diagnostic purposes.
 * Maps to HTTP 422 via GlobalExceptionHandler.
 */
public class TransformationExecutionException extends RuntimeException {

    private final String ruleName;
    private final TransformationRule.TransformationType ruleType;

    public TransformationExecutionException(TransformationRule rule, String message) {
        super(String.format("[Rule '%s' / %s] %s",
                rule.getName(), rule.getTransformationType(), message));
        this.ruleName = rule.getName();
        this.ruleType = rule.getTransformationType();
    }

    public TransformationExecutionException(TransformationRule rule, String message, Throwable cause) {
        super(String.format("[Rule '%s' / %s] %s",
                rule.getName(), rule.getTransformationType(), message), cause);
        this.ruleName = rule.getName();
        this.ruleType = rule.getTransformationType();
    }

    public String getRuleName() { return ruleName; }
    public TransformationRule.TransformationType getRuleType() { return ruleType; }
}
