package com.company.integrationplatform.transformation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stateless transformation engine.
 *
 * <p>Applies a single {@link TransformationRule} to a data record map and returns
 * a new map with the transformation applied. The input map is never mutated.
 *
 * <p>Each transformation type reads from {@code sourceField} and writes to
 * {@code targetField} (or overwrites {@code sourceField} if targetField is null).
 *
 * <p>If the sourceField is absent from the record, the rule is silently skipped
 * (no exception) unless the type is {@code DEFAULT_VALUE}, which always writes.
 */
@Slf4j
@Component
public class TransformationEngine {

    /**
     * Applies a single rule to the given record.
     *
     * @param record the input data map (not mutated)
     * @param rule   the rule to apply
     * @return a new map with the transformation applied
     * @throws TransformationExecutionException if the rule fails due to bad data
     */
    public Map<String, Object> apply(Map<String, Object> record, TransformationRule rule) {
        Map<String, Object> result = new LinkedHashMap<>(record);

        String src    = rule.getSourceField();
        String target = (rule.getTargetField() != null && !rule.getTargetField().isBlank())
                        ? rule.getTargetField()
                        : src;

        try {
            switch (rule.getTransformationType()) {

                case DIRECT_MAPPING -> {
                    // Copy sourceField value to targetField unchanged
                    requireSourceField(src, rule);
                    if (result.containsKey(src)) {
                        result.put(target, result.get(src));
                        if (!target.equals(src)) result.remove(src);
                    }
                }

                case UPPERCASE -> {
                    requireSourceField(src, rule);
                    if (result.containsKey(src)) {
                        String val = toStr(result.get(src));
                        result.put(target, val.toUpperCase());
                        if (!target.equals(src)) result.remove(src);
                    }
                }

                case LOWERCASE -> {
                    requireSourceField(src, rule);
                    if (result.containsKey(src)) {
                        String val = toStr(result.get(src));
                        result.put(target, val.toLowerCase());
                        if (!target.equals(src)) result.remove(src);
                    }
                }

                case TRIM -> {
                    requireSourceField(src, rule);
                    if (result.containsKey(src)) {
                        String val = toStr(result.get(src));
                        result.put(target, val.trim());
                        if (!target.equals(src)) result.remove(src);
                    }
                }

                case CONCAT -> {
                    // Concatenate sourceField + extraConfig (second field) with defaultValue as separator
                    requireSourceField(src, rule);
                    String secondField = rule.getExtraConfig();
                    String separator   = rule.getDefaultValue() != null ? rule.getDefaultValue() : "";

                    String part1 = result.containsKey(src)
                            ? toStr(result.get(src)) : "";
                    String part2 = (secondField != null && result.containsKey(secondField))
                            ? toStr(result.get(secondField)) : "";

                    result.put(target, part1 + separator + part2);
                }

                case DEFAULT_VALUE -> {
                    // Write defaultValue to targetField when sourceField is null/blank
                    String currentVal = result.containsKey(src)
                            ? toStr(result.get(src)) : null;

                    if (currentVal == null || currentVal.isBlank()) {
                        result.put(target, rule.getDefaultValue() != null
                                ? rule.getDefaultValue() : "");
                    } else {
                        result.put(target, currentVal);
                        if (!target.equals(src)) result.remove(src);
                    }
                }

                case DATE_FORMAT -> {
                    // Parse sourceField with extraConfig pattern, reformat with defaultValue pattern
                    requireSourceField(src, rule);
                    if (result.containsKey(src)) {
                        String inputPattern  = rule.getExtraConfig();
                        String outputPattern = rule.getDefaultValue();

                        if (inputPattern == null || outputPattern == null) {
                            throw new TransformationExecutionException(rule,
                                    "DATE_FORMAT requires extraConfig (input pattern) and defaultValue (output pattern)");
                        }

                        String rawDate = toStr(result.get(src));
                        String formatted = reformatDate(rawDate, inputPattern, outputPattern, rule);
                        result.put(target, formatted);
                        if (!target.equals(src)) result.remove(src);
                    }
                }
            }
        } catch (TransformationExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new TransformationExecutionException(rule,
                    "Unexpected error: " + e.getMessage(), e);
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String reformatDate(String rawDate, String inputPattern,
                                 String outputPattern, TransformationRule rule) {
        try {
            DateTimeFormatter inputFmt  = DateTimeFormatter.ofPattern(inputPattern);
            DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern(outputPattern);

            // Try LocalDateTime first, fall back to LocalDate
            try {
                LocalDateTime dt = LocalDateTime.parse(rawDate, inputFmt);
                return dt.format(outputFmt);
            } catch (DateTimeParseException ex) {
                LocalDate d = LocalDate.parse(rawDate, inputFmt);
                return d.format(outputFmt);
            }
        } catch (Exception e) {
            throw new TransformationExecutionException(rule,
                    String.format("Cannot parse date '%s' with pattern '%s': %s",
                            rawDate, inputPattern, e.getMessage()));
        }
    }

    private void requireSourceField(String sourceField, TransformationRule rule) {
        if (sourceField == null || sourceField.isBlank()) {
            throw new TransformationExecutionException(rule,
                    "sourceField is required for transformation type " + rule.getTransformationType());
        }
    }

    private String toStr(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
