package com.company.integrationplatform.datasource;

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

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Data Source Management module.
 *
 * <p><b>RBAC Summary:</b>
 * <ul>
 *   <li>CREATE  — ADMIN, ANALYST, OPERATOR</li>
 *   <li>READ    — ADMIN, ANALYST, OPERATOR</li>
 *   <li>UPDATE  — ADMIN, ANALYST</li>
 *   <li>DELETE  — ADMIN only</li>
 * </ul>
 *
 * <p>Authorization is enforced at method level via {@code @PreAuthorize}.
 * No class-level annotation is used so each endpoint's policy is explicit.
 */
@RestController
@RequestMapping("/api/v1/data-sources")
@RequiredArgsConstructor
@Tag(
    name = "Data Source Management",
    description = "APIs for registering, querying, updating, and deleting external data source definitions. "
                + "Supports CSV, REST_API, and DATABASE source types. "
                + "RBAC: CREATE=ADMIN/ANALYST/OPERATOR, READ=all, UPDATE=ADMIN/ANALYST, DELETE=ADMIN only."
)
@SecurityRequirement(name = "bearerAuth")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a new external data source definition.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     * <p><b>Security rationale:</b> All operational roles need to register data sources
     * as part of their day-to-day ingestion workflows.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Register a new data source",
        description = "Creates a new data source definition. Status defaults to INACTIVE. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DataSourceDto.CreateRequest.class),
                examples = {
                    @ExampleObject(
                        name = "DATABASE source",
                        summary = "PostgreSQL database connection",
                        value = """
                            {
                              "name": "Production PostgreSQL",
                              "sourceType": "DATABASE",
                              "description": "Main production database",
                              "connectionDetails": {
                                "host": "db.example.com",
                                "port": "5432",
                                "database": "prod_db",
                                "username": "reader",
                                "password": "secret"
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "REST_API source",
                        summary = "External REST API endpoint",
                        value = """
                            {
                              "name": "Customer Orders API",
                              "sourceType": "REST_API",
                              "description": "Fetches customer orders from the orders service",
                              "connectionDetails": {
                                "url": "https://api.example.com/v1/orders",
                                "authType": "BEARER",
                                "token": "eyJhbGciOiJIUzI1NiJ9..."
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "CSV source",
                        summary = "CSV file ingestion source",
                        value = """
                            {
                              "name": "Monthly Sales CSV",
                              "sourceType": "CSV",
                              "description": "Monthly sales data uploaded as CSV",
                              "connectionDetails": {
                                "delimiter": ",",
                                "encoding": "UTF-8",
                                "hasHeader": "true"
                              }
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Data source created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — name missing, invalid sourceType, or duplicate name"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<DataSourceDto.Response>> create(
            @Valid @RequestBody DataSourceDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Data source created successfully",
                        dataSourceService.create(request)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the full details of a single data source including connection parameters.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get data source by ID",
        description = "Returns full data source details including connection parameters. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Data source found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Data source not found")
    })
    public ResponseEntity<ApiResponse<DataSourceDto.Response>> getById(
            @Parameter(description = "Data source UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(dataSourceService.getById(id)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — LIST ALL (paginated)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all data sources as lightweight summaries.
     * Connection details are excluded from list responses for security.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "List all data sources (paginated)",
        description = "Returns a paginated list of data source summaries. "
                    + "Connection details are excluded from list responses. "
                    + "Supports sorting by: name, sourceType, status, createdAt. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<PageResponse<DataSourceDto.Summary>>> getAll(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field: name | sourceType | status | createdAt", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction: asc | desc", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = buildSort(sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success(
                dataSourceService.getAll(PageRequest.of(page, size, sort))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — SEARCH
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Searches data sources by optional name (partial match), sourceType, and/or status.
     * All search parameters are optional — omitting all returns all data sources.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Search data sources",
        description = "Searches data sources by optional filters. All parameters are optional. "
                    + "Name search is case-insensitive and partial (contains). "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**\n\n"
                    + "**Example:** `GET /api/v1/data-sources/search?name=prod&sourceType=DATABASE&status=ACTIVE`"
    )
    public ResponseEntity<ApiResponse<PageResponse<DataSourceDto.Summary>>> search(
            @Parameter(description = "Partial name search (case-insensitive)", example = "prod")
            @RequestParam(required = false) String name,

            @Parameter(description = "Filter by source type: CSV | REST_API | DATABASE", example = "DATABASE")
            @RequestParam(required = false) DataSourceEntity.SourceType sourceType,

            @Parameter(description = "Filter by status: ACTIVE | INACTIVE | ERROR", example = "ACTIVE")
            @RequestParam(required = false) DataSourceEntity.SourceStatus status,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field: name | sourceType | status | createdAt", example = "name")
            @RequestParam(defaultValue = "name") String sortBy,

            @Parameter(description = "Sort direction: asc | desc", example = "asc")
            @RequestParam(defaultValue = "asc") String direction) {

        DataSourceSearchCriteria criteria = DataSourceSearchCriteria.builder()
                .name(name)
                .sourceType(sourceType)
                .status(status)
                .build();

        Sort sort = buildSort(sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success(
                dataSourceService.search(criteria, PageRequest.of(page, size, sort))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — ACTIVE SOURCES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all ACTIVE data sources as a flat list (no pagination).
     * Used by the ingestion and synchronization modules.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get all active data sources",
        description = "Returns all data sources with status=ACTIVE as a flat list. "
                    + "Used by ingestion and sync workflows. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<List<DataSourceDto.Summary>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(dataSourceService.getActive()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Partially updates a data source. Only provided (non-null) fields are applied.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST
     * <p><b>Security rationale:</b> Modifying connection details or status can affect
     * live ingestion pipelines. Restricted to privileged roles.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "Update a data source",
        description = "Partially updates a data source. Only provided fields are changed. "
                    + "**Allowed roles: ADMIN, ANALYST** — OPERATOR is denied.\n\n"
                    + "**Example — activate a source:**\n"
                    + "```json\n{ \"status\": \"ACTIVE\" }\n```\n\n"
                    + "**Example — update connection and description:**\n"
                    + "```json\n{\n  \"description\": \"Switched to replica\",\n"
                    + "  \"connectionDetails\": { \"host\": \"replica.db.com\" }\n}\n```",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DataSourceDto.UpdateRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Activate source",
                        summary = "Set status to ACTIVE",
                        value = """
                            {
                              "status": "ACTIVE"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Update connection",
                        summary = "Update host and description",
                        value = """
                            {
                              "description": "Switched to read replica",
                              "connectionDetails": {
                                "host": "replica.db.example.com",
                                "port": "5432"
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Mark as error",
                        summary = "Flag source as errored",
                        value = """
                            {
                              "status": "ERROR"
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Data source updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions — OPERATOR cannot update"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Data source not found")
    })
    public ResponseEntity<ApiResponse<DataSourceDto.Response>> update(
            @Parameter(description = "Data source UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody DataSourceDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Data source updated successfully",
                dataSourceService.update(id, request)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes a data source.
     *
     * <p><b>Allowed roles:</b> ADMIN only
     * <p><b>Security rationale:</b> Deleting a data source is irreversible and
     * may break active ingestion pipelines. Restricted to ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete a data source",
        description = "Permanently deletes a data source. This action is irreversible. "
                    + "**Allowed roles: ADMIN only** — ANALYST and OPERATOR are denied."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Data source deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions — only ADMIN can delete"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Data source not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Data source UUID", required = true)
            @PathVariable UUID id) {
        dataSourceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Data source deleted successfully", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a {@link Sort} object from string parameters.
     * Defaults to descending if direction is unrecognised.
     */
    private Sort buildSort(String sortBy, String direction) {
        return "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
    }
}
