package com.company.integrationplatform.ingestion;

import com.company.integrationplatform.common.ApiResponse;
import com.company.integrationplatform.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for the CSV Ingestion module.
 *
 * <p><b>RBAC Summary:</b>
 * <ul>
 *   <li>UPLOAD CSV  — ADMIN, ANALYST</li>
 *   <li>VIEW JOBS   — ADMIN, ANALYST, OPERATOR</li>
 * </ul>
 *
 * <p>Authorization is enforced at method level via {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
@Tag(
    name = "CSV Data Ingestion",
    description = "APIs for uploading CSV files, triggering ingestion jobs, and monitoring job status. "
                + "RBAC: UPLOAD=ADMIN/ANALYST, VIEW=ADMIN/ANALYST/OPERATOR."
)
@SecurityRequirement(name = "bearerAuth")
public class IngestionController {

    private final IngestionService ingestionService;

    // ─────────────────────────────────────────────────────────────────────────
    // UPLOAD & INGEST
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uploads a CSV file and immediately starts an ingestion job.
     *
     * <p><b>Validation:</b>
     * <ul>
     *   <li>File must not be empty</li>
     *   <li>File must be CSV (.csv extension or text/csv content type)</li>
     *   <li>File must not exceed 10 MB</li>
     *   <li>Data source must exist and be ACTIVE</li>
     *   <li>CSV must have a header row</li>
     *   <li>CSV must have at least one data row</li>
     * </ul>
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST
     * <p><b>Security rationale:</b> Uploading data is a privileged operation that
     * can affect downstream pipelines. OPERATOR can view jobs but not trigger uploads.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "Upload a CSV file and start ingestion",
        description = """
            Uploads a CSV file and immediately starts an ingestion job for the specified data source.

            **Validation rules:**
            - File must not be empty
            - File must be CSV (`.csv` extension or `text/csv` content type)
            - Maximum file size: **10 MB**
            - Data source must exist and have status `ACTIVE`
            - CSV must contain a **header row**
            - CSV must contain **at least one data row**

            **Audit events emitted:** `UPLOAD_CSV` → `INGESTION_STARTED` → `INGESTION_COMPLETED` or `INGESTION_FAILED`

            **Allowed roles: ADMIN, ANALYST** — OPERATOR is denied.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Ingestion job created and completed (or partially completed)",
            content = @Content(schema = @Schema(implementation = IngestionDto.JobResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid file type, empty file, or missing header row"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions — OPERATOR cannot upload"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Data source not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "422",
            description = "CSV validation failed — malformed CSV, file too large, or data source not ACTIVE"
        )
    })
    public ResponseEntity<ApiResponse<IngestionDto.JobResponse>> uploadCsv(
            @Parameter(
                description = "UUID of the ACTIVE data source to associate this upload with",
                required = true,
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7"
            )
            @RequestParam("dataSourceId") UUID dataSourceId,

            @Parameter(
                description = "CSV file to upload. Must be .csv, max 10 MB, with a header row.",
                required = true,
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file) {

        IngestionDto.JobResponse response =
                ingestionService.uploadAndIngestCsv(dataSourceId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CSV ingestion job completed", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIST ALL JOBS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all ingestion jobs, newest first.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "List all ingestion jobs (paginated)",
        description = "Returns a paginated list of all ingestion jobs sorted by creation time. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Paginated list of ingestion jobs"
        )
    })
    public ResponseEntity<ApiResponse<PageResponse<IngestionDto.JobResponse>>> getAllJobs(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort direction: asc | desc", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        return ResponseEntity.ok(ApiResponse.success(
                ingestionService.getAllJobs(PageRequest.of(page, size, sort))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET JOB BY ID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns full details of a single ingestion job including failed row diagnostics.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get ingestion job details by ID",
        description = "Returns full job details including status, record counts, and up to 100 failed row diagnostics. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Job details found",
            content = @Content(schema = @Schema(implementation = IngestionDto.JobDetailResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Ingestion job not found"
        )
    })
    public ResponseEntity<ApiResponse<IngestionDto.JobDetailResponse>> getJobById(
            @Parameter(description = "Ingestion job UUID", required = true)
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(ingestionService.getJobById(jobId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET JOBS BY DATA SOURCE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns paginated ingestion jobs for a specific data source.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/jobs/source/{dataSourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get ingestion jobs for a data source",
        description = "Returns paginated ingestion jobs associated with a specific data source. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Paginated list of jobs for the data source"
        )
    })
    public ResponseEntity<ApiResponse<PageResponse<IngestionDto.JobResponse>>> getJobsBySource(
            @Parameter(description = "Data source UUID", required = true)
            @PathVariable UUID dataSourceId,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort direction: asc | desc", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        return ResponseEntity.ok(ApiResponse.success(
                ingestionService.getJobsByDataSource(dataSourceId,
                        PageRequest.of(page, size, sort))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REST API INGESTION (existing endpoint — kept)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Triggers ingestion from an external REST API data source.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @PostMapping("/api/{dataSourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Trigger REST API ingestion",
        description = "Fetches data from the URL configured in the data source and ingests it. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<IngestionDto.JobResponse>> ingestFromApi(
            @Parameter(description = "Data source UUID", required = true)
            @PathVariable UUID dataSourceId) {
        return ResponseEntity.ok(ApiResponse.success("API ingestion started",
                ingestionService.ingestFromApi(dataSourceId)));
    }
}
