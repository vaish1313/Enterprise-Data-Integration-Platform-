package com.company.integrationplatform.ingestion;

import com.company.integrationplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
@Tag(name = "Data Ingestion", description = "APIs for importing data from CSV files and external REST APIs")
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping(value = "/csv/{dataSourceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(summary = "Ingest data from a CSV file upload")
    public ResponseEntity<ApiResponse<IngestionDto.JobResponse>> ingestCsv(
            @PathVariable UUID dataSourceId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("CSV ingestion started",
                ingestionService.ingestCsv(dataSourceId, file)));
    }

    @PostMapping("/api/{dataSourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(summary = "Ingest data from an external REST API data source")
    public ResponseEntity<ApiResponse<IngestionDto.JobResponse>> ingestFromApi(
            @PathVariable UUID dataSourceId) {
        return ResponseEntity.ok(ApiResponse.success("API ingestion started",
                ingestionService.ingestFromApi(dataSourceId)));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get ingestion job status by ID")
    public ResponseEntity<ApiResponse<IngestionDto.JobResponse>> getJob(
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(ingestionService.getJobById(jobId)));
    }

    @GetMapping("/jobs/source/{dataSourceId}")
    @Operation(summary = "Get all ingestion jobs for a data source")
    public ResponseEntity<ApiResponse<List<IngestionDto.JobResponse>>> getJobsBySource(
            @PathVariable UUID dataSourceId) {
        return ResponseEntity.ok(ApiResponse.success(
                ingestionService.getJobsByDataSource(dataSourceId)));
    }
}
