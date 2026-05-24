package com.company.integrationplatform.transformation;

import com.company.integrationplatform.common.ApiResponse;
import com.company.integrationplatform.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the Transformation Rules Engine.
 *
 * <p><b>RBAC Summary:</b>
 * <ul>
 *   <li>CREATE / UPDATE / DELETE — ADMIN, ANALYST</li>
 *   <li>READ / APPLY             — ADMIN, ANALYST, OPERATOR</li>
 * </ul>
 *
 * <p>Authorization is enforced at method level via {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/v1/transformation-rules")
@RequiredArgsConstructor
@Tag(
    name = "Transformation Rules Engine",
    description = """
        APIs for managing and applying data transformation rules.
        Rules transform ingested records into a standardized enterprise format.

        **Supported transformation types:**
        | Type | Description |
        |------|-------------|
        | `DIRECT_MAPPING` | Copy sourceField to targetField unchanged |
        | `UPPERCASE` | Convert value to UPPER CASE |
        | `LOWERCASE` | Convert value to lower case |
        | `TRIM` | Strip leading/trailing whitespace |
        | `CONCAT` | Concatenate two fields with a separator |
        | `DEFAULT_VALUE` | Write fallback value when field is null/blank |
        | `DATE_FORMAT` | Reformat a date string from one pattern to another |

        **RBAC:** CREATE/UPDATE/DELETE = ADMIN/ANALYST, READ/APPLY = ADMIN/ANALYST/OPERATOR
        """
)
@SecurityRequirement(name = "bearerAuth")
public class TransformationController {

    private final TransformationService transformationService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new transformation rule.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST
     * <p><b>Security rationale:</b> Transformation rules affect how data is stored
     * and interpreted. Restricted to privileged roles to prevent accidental data corruption.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "Create a transformation rule",
        description = "Creates a new transformation rule. **Allowed roles: ADMIN, ANALYST**",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransformationDto.CreateRequest.class),
                examples = {
                    @ExampleObject(name = "TRIM whitespace",
                        summary = "Trim customer_name field",
                        value = """
                            {
                              "name": "Trim Customer Name",
                              "description": "Remove leading/trailing whitespace from customer_name",
                              "transformationType": "TRIM",
                              "sourceField": "customer_name",
                              "targetField": "customerName",
                              "executionOrder": 10
                            }
                            """),
                    @ExampleObject(name = "UPPERCASE",
                        summary = "Uppercase country code",
                        value = """
                            {
                              "name": "Uppercase Country Code",
                              "transformationType": "UPPERCASE",
                              "sourceField": "country",
                              "targetField": "countryCode",
                              "executionOrder": 20
                            }
                            """),
                    @ExampleObject(name = "CONCAT full name",
                        summary = "Concatenate first and last name",
                        value = """
                            {
                              "name": "Build Full Name",
                              "transformationType": "CONCAT",
                              "sourceField": "first_name",
                              "targetField": "fullName",
                              "extraConfig": "last_name",
                              "defaultValue": " ",
                              "executionOrder": 30
                            }
                            """),
                    @ExampleObject(name = "DEFAULT_VALUE",
                        summary = "Default status when missing",
                        value = """
                            {
                              "name": "Default Status",
                              "transformationType": "DEFAULT_VALUE",
                              "sourceField": "status",
                              "targetField": "status",
                              "defaultValue": "UNKNOWN",
                              "executionOrder": 40
                            }
                            """),
                    @ExampleObject(name = "DATE_FORMAT",
                        summary = "Reformat date from dd/MM/yyyy to yyyy-MM-dd",
                        value = """
                            {
                              "name": "Normalize Order Date",
                              "transformationType": "DATE_FORMAT",
                              "sourceField": "order_date",
                              "targetField": "orderDate",
                              "extraConfig": "dd/MM/yyyy",
                              "defaultValue": "yyyy-MM-dd",
                              "executionOrder": 50
                            }
                            """),
                    @ExampleObject(name = "DIRECT_MAPPING",
                        summary = "Rename a field",
                        value = """
                            {
                              "name": "Map cust_id to customerId",
                              "transformationType": "DIRECT_MAPPING",
                              "sourceField": "cust_id",
                              "targetField": "customerId",
                              "executionOrder": 5
                            }
                            """)
                }
            )
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
            description = "Rule created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Validation failed — missing required fields for the transformation type"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<TransformationDto.Response>> create(
            @Valid @RequestBody TransformationDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transformation rule created",
                        transformationService.createRule(request)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — LIST ALL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all transformation rules.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "List all transformation rules (paginated)",
        description = "Returns a paginated list of all transformation rules. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<PageResponse<TransformationDto.Response>>> getAll(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field: name | executionOrder | createdAt", example = "executionOrder")
            @RequestParam(defaultValue = "executionOrder") String sortBy,

            @Parameter(description = "Sort direction: asc | desc", example = "asc")
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = buildSort(sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success(
                transformationService.getAllRules(PageRequest.of(page, size, sort))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a single transformation rule by ID.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get transformation rule by ID",
        description = "Returns full details of a single transformation rule. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Rule found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Rule not found")
    })
    public ResponseEntity<ApiResponse<TransformationDto.Response>> getById(
            @Parameter(description = "Rule UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(transformationService.getRuleById(id)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — BY DATA SOURCE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all transformation rules scoped to a specific data source.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/source/{dataSourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get transformation rules for a data source",
        description = "Returns all rules scoped to a specific data source (excludes global rules). "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<PageResponse<TransformationDto.Response>>> getByDataSource(
            @Parameter(description = "Data source UUID", required = true)
            @PathVariable UUID dataSourceId,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "executionOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = buildSort(sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success(
                transformationService.getRulesByDataSource(dataSourceId,
                        PageRequest.of(page, size, sort))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Partially updates a transformation rule.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST
     * <p><b>Security rationale:</b> Same as create — rule changes affect data integrity.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "Update a transformation rule",
        description = "Partially updates a rule. Only provided fields are changed. "
                    + "**Allowed roles: ADMIN, ANALYST** — OPERATOR is denied.\n\n"
                    + "**Example — disable a rule:**\n```json\n{ \"active\": false }\n```\n\n"
                    + "**Example — change execution order:**\n```json\n{ \"executionOrder\": 15 }\n```",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransformationDto.UpdateRequest.class),
                examples = {
                    @ExampleObject(name = "Disable rule",
                        value = "{ \"active\": false }"),
                    @ExampleObject(name = "Change order",
                        value = "{ \"executionOrder\": 15 }"),
                    @ExampleObject(name = "Update default value",
                        value = "{ \"defaultValue\": \"N/A\" }")
                }
            )
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Rule updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Insufficient permissions — OPERATOR cannot update"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Rule not found")
    })
    public ResponseEntity<ApiResponse<TransformationDto.Response>> update(
            @Parameter(description = "Rule UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody TransformationDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Rule updated",
                transformationService.updateRule(id, request)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes a transformation rule.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST
     * <p><b>Security rationale:</b> Deletion is irreversible. Restricted to
     * privileged roles to prevent accidental removal of production rules.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "Delete a transformation rule",
        description = "Permanently deletes a transformation rule. This action is irreversible. "
                    + "**Allowed roles: ADMIN, ANALYST** — OPERATOR is denied."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Rule deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Rule not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Rule UUID", required = true)
            @PathVariable UUID id) {
        transformationService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Rule deleted", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPLY TO JOB
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Re-applies all active transformation rules to every record of an existing ingestion job.
     * Useful when rules are added or changed after ingestion has already run.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @PostMapping("/apply/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Apply transformation rules to an ingestion job",
        description = """
            Re-applies all active transformation rules to every record of the specified ingestion job.
            Updates the `transformedData` field on each record.

            Use this endpoint when:
            - New rules were added after ingestion ran
            - Existing rules were modified
            - You want to re-transform records with the latest rule set

            Returns a detailed summary including per-rule applied/skipped/error counts.

            **Allowed roles: ADMIN, ANALYST, OPERATOR**
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Transformation applied successfully",
            content = @Content(schema = @Schema(implementation = TransformationDto.ApplyResult.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Ingestion job not found")
    })
    public ResponseEntity<ApiResponse<TransformationDto.ApplyResult>> applyToJob(
            @Parameter(description = "Ingestion job UUID to apply rules to", required = true)
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success("Transformation applied",
                transformationService.applyToJob(jobId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private Sort buildSort(String sortBy, String direction) {
        return "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
    }
}
