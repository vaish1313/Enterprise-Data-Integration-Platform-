package com.company.integrationplatform.datasource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for masking sensitive credential fields in connection detail maps
 * before they are serialised into API responses.
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li>Masking happens at the DTO layer — the entity and database always store
 *       the real values. Only the outbound API response is sanitised.</li>
 *   <li>A centralised set of {@link #SENSITIVE_KEYS} ensures every new field
 *       only needs to be added in one place.</li>
 *   <li>Keys are matched case-insensitively so {@code "Password"}, {@code "PASSWORD"},
 *       and {@code "password"} are all caught.</li>
 *   <li>The masked value {@code "******"} is a fixed sentinel — never the real length
 *       of the secret, which would leak information.</li>
 *   <li>Null-safe: a null or empty map is returned as-is.</li>
 * </ul>
 */
public final class CredentialMaskingUtil {

    /** Replacement value used for all masked fields. */
    public static final String MASK = "******";

    /**
     * Case-insensitive set of connection detail keys whose values must never
     * appear in API responses.
     *
     * <p>Add new sensitive key names here — masking is applied automatically
     * everywhere {@link #mask(Map)} is called.
     */
    static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "passwd",
            "secret",
            "token",
            "apikey",
            "api_key",
            "accesskey",
            "access_key",
            "secretkey",
            "secret_key",
            "privatekey",
            "private_key",
            "clientsecret",
            "client_secret",
            "authtoken",
            "auth_token",
            "bearertoken",
            "bearer_token"
    );

    private CredentialMaskingUtil() {}

    /**
     * Returns a new map with all sensitive values replaced by {@link #MASK}.
     * Non-sensitive keys are copied as-is. The original map is never modified.
     *
     * @param connectionDetails the raw connection detail map from the entity
     * @return a sanitised copy safe for inclusion in API responses,
     *         or {@code null} if the input is {@code null}
     */
    public static Map<String, String> mask(Map<String, String> connectionDetails) {
        if (connectionDetails == null || connectionDetails.isEmpty()) {
            return connectionDetails;
        }

        Map<String, String> masked = new HashMap<>(connectionDetails.size());
        connectionDetails.forEach((key, value) -> {
            if (isSensitive(key)) {
                masked.put(key, MASK);
            } else {
                masked.put(key, value);
            }
        });
        return Collections.unmodifiableMap(masked);
    }

    /**
     * Returns {@code true} if the given key name matches any entry in
     * {@link #SENSITIVE_KEYS} (case-insensitive comparison).
     *
     * @param key the connection detail key to check
     * @return {@code true} if the key is considered sensitive
     */
    public static boolean isSensitive(String key) {
        if (key == null) return false;
        return SENSITIVE_KEYS.contains(key.toLowerCase());
    }
}
