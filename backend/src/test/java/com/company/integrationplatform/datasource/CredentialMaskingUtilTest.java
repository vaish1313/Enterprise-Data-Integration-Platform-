package com.company.integrationplatform.datasource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static com.company.integrationplatform.datasource.CredentialMaskingUtil.MASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CredentialMaskingUtil}.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>All known sensitive keys are masked in API responses</li>
 *   <li>Non-sensitive keys pass through unchanged</li>
 *   <li>The original map is never mutated</li>
 *   <li>Null and empty maps are handled safely</li>
 *   <li>Key matching is case-insensitive</li>
 * </ul>
 */
@DisplayName("CredentialMaskingUtil")
class CredentialMaskingUtilTest {

    // ─────────────────────────────────────────────────────────────────────────
    // mask() — sensitive key masking
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mask()")
    class MaskTests {

        @Test
        @DisplayName("masks 'password' field and keeps non-sensitive fields unchanged")
        void masksPasswordKeepsOtherFields() {
            Map<String, String> input = new HashMap<>();
            input.put("host", "db.example.com");
            input.put("port", "5432");
            input.put("database", "prod_db");
            input.put("username", "reader");
            input.put("password", "super_secret_password");

            Map<String, String> result = CredentialMaskingUtil.mask(input);

            assertThat(result.get("host")).isEqualTo("db.example.com");
            assertThat(result.get("port")).isEqualTo("5432");
            assertThat(result.get("database")).isEqualTo("prod_db");
            assertThat(result.get("username")).isEqualTo("reader");
            assertThat(result.get("password")).isEqualTo(MASK);
        }

        @ParameterizedTest(name = "masks sensitive key: ''{0}''")
        @ValueSource(strings = {
                "password", "passwd", "secret", "token",
                "apikey", "api_key", "accesskey", "access_key",
                "secretkey", "secret_key", "privatekey", "private_key",
                "clientsecret", "client_secret", "authtoken", "auth_token",
                "bearertoken", "bearer_token"
        })
        @DisplayName("masks all known sensitive key names")
        void masksAllSensitiveKeys(String sensitiveKey) {
            Map<String, String> input = Map.of(sensitiveKey, "real_secret_value");

            Map<String, String> result = CredentialMaskingUtil.mask(input);

            assertThat(result.get(sensitiveKey))
                    .as("Key '%s' should be masked", sensitiveKey)
                    .isEqualTo(MASK);
        }

        @ParameterizedTest(name = "masks case-insensitive variant: ''{0}''")
        @ValueSource(strings = {"PASSWORD", "Password", "PASSWD", "Token", "TOKEN", "Secret", "SECRET"})
        @DisplayName("masking is case-insensitive")
        void maskingIsCaseInsensitive(String key) {
            Map<String, String> input = Map.of(key, "real_value");

            Map<String, String> result = CredentialMaskingUtil.mask(input);

            assertThat(result.get(key))
                    .as("Key '%s' should be masked regardless of case", key)
                    .isEqualTo(MASK);
        }

        @Test
        @DisplayName("does not expose the real password value in the masked map")
        void doesNotExposeRealPassword() {
            String realPassword = "my_real_database_password_123";
            Map<String, String> input = Map.of("password", realPassword);

            Map<String, String> result = CredentialMaskingUtil.mask(input);

            assertThat(result.values())
                    .as("Real password must not appear in any response value")
                    .doesNotContain(realPassword);
        }

        @Test
        @DisplayName("does not expose real token value")
        void doesNotExposeRealToken() {
            String realToken = "eyJhbGciOiJIUzI1NiJ9.realPayload.signature";
            Map<String, String> input = Map.of(
                    "url", "https://api.example.com",
                    "token", realToken
            );

            Map<String, String> result = CredentialMaskingUtil.mask(input);

            assertThat(result.get("token")).isEqualTo(MASK);
            assertThat(result.values()).doesNotContain(realToken);
        }

        @Test
        @DisplayName("masked value is always the fixed sentinel '******', not the real length")
        void maskedValueIsFixedSentinel() {
            Map<String, String> input = Map.of("password", "short");
            Map<String, String> input2 = Map.of("password", "a_very_long_password_that_is_64_characters_long_xxxxxxxxxxxxxxxxx");

            assertThat(CredentialMaskingUtil.mask(input).get("password")).isEqualTo(MASK);
            assertThat(CredentialMaskingUtil.mask(input2).get("password")).isEqualTo(MASK);
        }

        @Test
        @DisplayName("non-sensitive keys are never masked")
        void nonSensitiveKeysPassThrough() {
            Map<String, String> input = Map.of(
                    "host", "db.example.com",
                    "port", "5432",
                    "database", "mydb",
                    "username", "reader",
                    "encoding", "UTF-8",
                    "delimiter", ","
            );

            Map<String, String> result = CredentialMaskingUtil.mask(input);

            assertThat(result.get("host")).isEqualTo("db.example.com");
            assertThat(result.get("port")).isEqualTo("5432");
            assertThat(result.get("database")).isEqualTo("mydb");
            assertThat(result.get("username")).isEqualTo("reader");
            assertThat(result.get("encoding")).isEqualTo("UTF-8");
            assertThat(result.get("delimiter")).isEqualTo(",");
        }

        @Test
        @DisplayName("original map is not mutated")
        void originalMapIsNotMutated() {
            Map<String, String> original = new HashMap<>();
            original.put("host", "db.example.com");
            original.put("password", "real_password");

            CredentialMaskingUtil.mask(original);

            assertThat(original.get("password"))
                    .as("Original map must not be modified")
                    .isEqualTo("real_password");
        }

        @Test
        @DisplayName("returned map is immutable")
        void returnedMapIsImmutable() {
            Map<String, String> input = Map.of("password", "secret");

            Map<String, String> result = CredentialMaskingUtil.mask(input);

            assertThatThrownBy(() -> result.put("newKey", "newValue"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("returns null when input is null")
        void returnsNullForNullInput() {
            assertThat(CredentialMaskingUtil.mask(null)).isNull();
        }

        @Test
        @DisplayName("returns empty map when input is empty")
        void returnsEmptyForEmptyInput() {
            assertThat(CredentialMaskingUtil.mask(Map.of())).isEmpty();
        }

        @Test
        @DisplayName("handles map with multiple sensitive keys simultaneously")
        void masksMultipleSensitiveKeysAtOnce() {
            Map<String, String> input = new HashMap<>();
            input.put("host", "db.example.com");
            input.put("password", "db_password");
            input.put("token", "api_token_value");
            input.put("secret", "app_secret");
            input.put("username", "reader");

            Map<String, String> result = CredentialMaskingUtil.mask(input);

            assertThat(result.get("password")).isEqualTo(MASK);
            assertThat(result.get("token")).isEqualTo(MASK);
            assertThat(result.get("secret")).isEqualTo(MASK);
            assertThat(result.get("host")).isEqualTo("db.example.com");
            assertThat(result.get("username")).isEqualTo("reader");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isSensitive()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isSensitive()")
    class IsSensitiveTests {

        @Test
        @DisplayName("returns true for 'password'")
        void trueForPassword() {
            assertThat(CredentialMaskingUtil.isSensitive("password")).isTrue();
        }

        @Test
        @DisplayName("returns true for 'token'")
        void trueForToken() {
            assertThat(CredentialMaskingUtil.isSensitive("token")).isTrue();
        }

        @Test
        @DisplayName("returns false for 'host'")
        void falseForHost() {
            assertThat(CredentialMaskingUtil.isSensitive("host")).isFalse();
        }

        @Test
        @DisplayName("returns false for 'username'")
        void falseForUsername() {
            assertThat(CredentialMaskingUtil.isSensitive("username")).isFalse();
        }

        @Test
        @DisplayName("returns false for null")
        void falseForNull() {
            assertThat(CredentialMaskingUtil.isSensitive(null)).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DataSourceDto.Response.from() — DTO mapping integration
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DataSourceDto.Response.from() — credential masking in DTO mapping")
    class DtoMappingTests {

        @Test
        @DisplayName("Response.from() masks password in connectionDetails")
        void responseDtoMasksPassword() {
            Map<String, String> connectionDetails = new HashMap<>();
            connectionDetails.put("host", "db.example.com");
            connectionDetails.put("port", "5432");
            connectionDetails.put("username", "reader");
            connectionDetails.put("password", "super_secret_db_password");

            DataSourceEntity entity = buildEntity(connectionDetails);

            DataSourceDto.Response response = DataSourceDto.Response.from(entity);

            assertThat(response.getConnectionDetails().get("password"))
                    .as("password must be masked in Response DTO")
                    .isEqualTo(MASK);
            assertThat(response.getConnectionDetails().get("host"))
                    .isEqualTo("db.example.com");
        }

        @Test
        @DisplayName("Response.from() masks token for REST_API sources")
        void responseDtoMasksToken() {
            Map<String, String> connectionDetails = new HashMap<>();
            connectionDetails.put("url", "https://api.example.com/v1/data");
            connectionDetails.put("authType", "BEARER");
            connectionDetails.put("token", "eyJhbGciOiJIUzI1NiJ9.payload.sig");

            DataSourceEntity entity = buildEntity(connectionDetails);

            DataSourceDto.Response response = DataSourceDto.Response.from(entity);

            assertThat(response.getConnectionDetails().get("token"))
                    .as("token must be masked in Response DTO")
                    .isEqualTo(MASK);
            assertThat(response.getConnectionDetails().get("url"))
                    .isEqualTo("https://api.example.com/v1/data");
            assertThat(response.getConnectionDetails().get("authType"))
                    .isEqualTo("BEARER");
        }

        @Test
        @DisplayName("Response.from() does not expose real password value anywhere in the response")
        void responseDtoNeverExposesRealPassword() {
            String realPassword = "never_expose_this_password_value";
            Map<String, String> connectionDetails = new HashMap<>();
            connectionDetails.put("password", realPassword);
            connectionDetails.put("host", "db.example.com");

            DataSourceEntity entity = buildEntity(connectionDetails);

            DataSourceDto.Response response = DataSourceDto.Response.from(entity);

            assertThat(response.getConnectionDetails().values())
                    .as("Real password must not appear in any response field")
                    .doesNotContain(realPassword);
        }

        @Test
        @DisplayName("Response.from() handles null connectionDetails safely")
        void responseDtoHandlesNullConnectionDetails() {
            DataSourceEntity entity = buildEntity(null);

            DataSourceDto.Response response = DataSourceDto.Response.from(entity);

            assertThat(response.getConnectionDetails()).isNull();
        }

        @Test
        @DisplayName("Summary.from() never includes connectionDetails at all")
        void summaryDtoExcludesConnectionDetails() {
            Map<String, String> connectionDetails = new HashMap<>();
            connectionDetails.put("password", "secret");
            connectionDetails.put("host", "db.example.com");

            DataSourceEntity entity = buildEntity(connectionDetails);

            DataSourceDto.Summary summary = DataSourceDto.Summary.from(entity);

            // Summary has no connectionDetails field by design.
            // Verify via reflection that no field named "connectionDetails" exists on Summary.
            boolean hasConnectionDetailsField = java.util.Arrays
                    .stream(DataSourceDto.Summary.class.getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("connectionDetails"));

            assertThat(hasConnectionDetailsField)
                    .as("Summary DTO must not declare a connectionDetails field")
                    .isFalse();

            // Also verify the summary carries the non-sensitive fields correctly
            assertThat(summary.getName()).isEqualTo("Test Source");
            assertThat(summary.getStatus()).isEqualTo(DataSourceEntity.SourceStatus.ACTIVE);
        }

        // ── helper ────────────────────────────────────────────────────────────

        private DataSourceEntity buildEntity(Map<String, String> connectionDetails) {
            DataSourceEntity entity = new DataSourceEntity();
            entity.setName("Test Source");
            entity.setSourceType(DataSourceEntity.SourceType.DATABASE);
            entity.setConnectionDetails(connectionDetails);
            entity.setStatus(DataSourceEntity.SourceStatus.ACTIVE);
            entity.setDescription("Test");
            entity.setCreatedBy("admin");
            return entity;
        }
    }
}
