package com.company.integrationplatform.ingestion;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.common.PageResponse;
import com.company.integrationplatform.datasource.DataSourceEntity;
import com.company.integrationplatform.datasource.DataSourceNotFoundException;
import com.company.integrationplatform.datasource.DataSourceRepository;
import com.company.integrationplatform.exception.ResourceNotFoundException;
import com.company.integrationplatform.notification.NotificationService;
import com.company.integrationplatform.transformation.TransformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service layer for the CSV Ingestion module.
 *
 * <p><b>CSV parsing</b> uses Apache Commons CSV with strict header validation.
 *
 * <p><b>Validation pipeline (in order):</b>
 * <ol>
 *   <li>File must not be null or empty</li>
 *   <li>File size must not exceed {@link Constants#MAX_CSV_FILE_SIZE_BYTES} (10 MB)</li>
 *   <li>File must have a {@code .csv} extension or {@code text/csv} content type</li>
 *   <li>Data source must exist and be ACTIVE</li>
 *   <li>CSV must have a header row</li>
 *   <li>CSV must have at least one data row</li>
 * </ol>
 *
 * <p><b>Audit events emitted:</b>
 * UPLOAD_CSV → INGESTION_STARTED → INGESTION_COMPLETED | INGESTION_FAILED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final IngestionRepository        ingestionRepository;
    private final IngestionRecordRepository  recordRepository;
    private final DataSourceRepository       dataSourceRepository;
    private final TransformationService      transformationService;
    private final AuditService               auditService;
    private final NotificationService        notificationService;
    private final RestTemplate               restTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // CSV UPLOAD & INGESTION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates, parses, and ingests a CSV file upload.
     *
     * @param dataSourceId the data source to associate this job with
     * @param file         the uploaded CSV file
     * @return the completed {@link IngestionDto.JobResponse}
     * @throws CsvIngestionException    if the file fails any validation check
     * @throws DataSourceNotFoundException if the data source does not exist
     */
    @org.springframework.scheduling.annotation.Async("jobExecutor")
    @Transactional
    public java.util.concurrent.CompletableFuture<IngestionDto.JobResponse> uploadAndIngestCsv(UUID dataSourceId, MultipartFile file) {
        String currentUser = currentUsername();

        // ── Step 1: File validation ───────────────────────────────────────────
        validateCsvFile(file);

        // ── Step 2: Data source validation ───────────────────────────────────
        DataSourceEntity source = getActiveSource(dataSourceId);

        // ── Step 3: Audit — upload received ──────────────────────────────────
        auditService.log(
                Constants.ACTION_UPLOAD_CSV,
                currentUser,
                "SUCCESS",
                String.format("CSV file uploaded: name='%s', size=%d bytes, dataSource='%s'",
                        file.getOriginalFilename(), file.getSize(), source.getName())
        );

        // ── Step 4: Create job record ─────────────────────────────────────────
        IngestionJob job = IngestionJob.builder()
                .dataSourceId(dataSourceId)
                .status(IngestionJob.JobStatus.RUNNING)
                .ingestionType(IngestionJob.IngestionType.CSV)
                .fileName(file.getOriginalFilename())
                .startedAt(LocalDateTime.now())
                .triggeredBy(currentUser)
                .build();
        job = ingestionRepository.save(job);

        auditService.log(
                Constants.ACTION_INGESTION_STARTED,
                currentUser,
                "RUNNING",
                String.format("Ingestion job started: jobId=%s, file='%s', dataSource='%s'",
                        job.getId(), file.getOriginalFilename(), source.getName())
        );

        log.info("CSV ingestion started: jobId={}, file={}, dataSource={}, user={}",
                job.getId(), file.getOriginalFilename(), source.getName(), currentUser);

        // ── Step 5: Parse and process ─────────────────────────────────────────
        int processed = 0, failed = 0, total = 0;
        List<IngestionRecord> records = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .setIgnoreSurroundingSpaces(true)
                     .build()
                     .parse(reader)) {

            // Validate header row exists
            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (headerMap == null || headerMap.isEmpty()) {
                throw new CsvIngestionException(
                        "CSV file has no header row. A header row is required.");
            }

            List<String> headers = new ArrayList<>(headerMap.keySet());
            log.debug("CSV headers detected: {}", headers);

            // Process each data row
            for (CSVRecord csvRecord : parser) {
                total++;
                int rowNum = (int) csvRecord.getRecordNumber(); // 1-based, header excluded

                try {
                    // Build raw data map from CSV record
                    Map<String, Object> rawData = new LinkedHashMap<>();
                    for (String header : headers) {
                        String value = csvRecord.isMapped(header) ? csvRecord.get(header) : null;
                        rawData.put(header, value);
                    }

                    // Apply transformation rules
                    Map<String, Object> transformed =
                            transformationService.transform(rawData, dataSourceId);

                    records.add(IngestionRecord.builder()
                            .jobId(job.getId())
                            .dataSourceId(dataSourceId)
                            .rawData(rawData)
                            .transformedData(transformed)
                            .status(IngestionRecord.RecordStatus.PROCESSED)
                            .sourceRowNumber(rowNum)
                            .build());
                    processed++;

                } catch (Exception e) {
                    log.warn("Row {} failed: {}", rowNum, e.getMessage());
                    records.add(IngestionRecord.builder()
                            .jobId(job.getId())
                            .dataSourceId(dataSourceId)
                            .status(IngestionRecord.RecordStatus.FAILED)
                            .errorMessage(truncate(e.getMessage(), 1000))
                            .sourceRowNumber(rowNum)
                            .build());
                    failed++;
                }

                // Batch-save every 500 records to avoid large heap usage
                if (records.size() >= 500) {
                    recordRepository.saveAll(records);
                    records.clear();
                }
            }

            // Save remaining records
            if (!records.isEmpty()) {
                recordRepository.saveAll(records);
            }

            if (total == 0) {
                throw new CsvIngestionException(
                        "CSV file contains a header row but no data rows.");
            }

            job.setStatus(failed == 0
                    ? IngestionJob.JobStatus.COMPLETED
                    : IngestionJob.JobStatus.PARTIAL);

        } catch (CsvIngestionException e) {
            // Structural validation failure — mark job as FAILED
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            log.error("CSV validation failed: jobId={}, error={}", job.getId(), e.getMessage());

            auditService.log(Constants.ACTION_INGESTION_FAILED, currentUser, "FAILED",
                    String.format("jobId=%s, error=%s", job.getId(), e.getMessage()));

            notificationService.createSystemNotification(
                    currentUser, "ERROR", "Ingestion Failed",
                    String.format("CSV validation failed for '%s': %s", file.getOriginalFilename(), truncate(e.getMessage(), 100)),
                    "INGESTION_JOB", job.getId()
            );

            // Persist job state before re-throwing
            job.setCompletedAt(LocalDateTime.now());
            job.setTotalRecords(total);
            job.setRecordsProcessed(processed);
            job.setRecordsFailed(failed);
            ingestionRepository.save(job);
            throw e;

        } catch (IOException e) {
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage("Failed to read CSV file: " + e.getMessage());
            log.error("CSV read error: jobId={}, error={}", job.getId(), e.getMessage());

            auditService.log(Constants.ACTION_INGESTION_FAILED, currentUser, "FAILED",
                    String.format("jobId=%s, ioError=%s", job.getId(), e.getMessage()));

            notificationService.createSystemNotification(
                    currentUser, "ERROR", "Ingestion Failed",
                    String.format("Failed to read CSV '%s': %s", file.getOriginalFilename(), truncate(e.getMessage(), 100)),
                    "INGESTION_JOB", job.getId()
            );

            job.setCompletedAt(LocalDateTime.now());
            job.setTotalRecords(total);
            job.setRecordsProcessed(processed);
            job.setRecordsFailed(failed);
            ingestionRepository.save(job);
            throw new CsvIngestionException("Failed to read CSV file: " + e.getMessage(), e);
        }

        // ── Step 6: Finalise job ──────────────────────────────────────────────
        job.setCompletedAt(LocalDateTime.now());
        job.setTotalRecords(total);
        job.setRecordsProcessed(processed);
        job.setRecordsFailed(failed);
        IngestionJob saved = ingestionRepository.save(job);

        auditService.log(
                Constants.ACTION_INGESTION_COMPLETED,
                currentUser,
                saved.getStatus().name(),
                String.format("jobId=%s, file='%s', total=%d, processed=%d, failed=%d",
                        saved.getId(), saved.getFileName(), total, processed, failed)
        );

        String type = saved.getStatus() == IngestionJob.JobStatus.COMPLETED ? "SUCCESS" : "WARNING";
        String title = type.equals("SUCCESS") ? "Ingestion Completed" : "Ingestion Partial";

        notificationService.createSystemNotification(
                currentUser, type, title,
                String.format("File '%s' processed. Total: %d, Failed: %d", saved.getFileName(), total, failed),
                "INGESTION_JOB", saved.getId()
        );

        log.info("CSV ingestion completed: jobId={}, total={}, processed={}, failed={}, status={}",
                saved.getId(), total, processed, failed, saved.getStatus());

        return java.util.concurrent.CompletableFuture.completedFuture(IngestionDto.JobResponse.from(saved));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REST API INGESTION (existing — kept intact)
    // ─────────────────────────────────────────────────────────────────────────

    @org.springframework.scheduling.annotation.Async("jobExecutor")
    @Transactional
    public java.util.concurrent.CompletableFuture<IngestionDto.JobResponse> ingestFromApi(UUID dataSourceId) {
        DataSourceEntity source = getActiveSource(dataSourceId);
        String currentUser = currentUsername();

        IngestionJob job = IngestionJob.builder()
                .dataSourceId(dataSourceId)
                .status(IngestionJob.JobStatus.RUNNING)
                .ingestionType(IngestionJob.IngestionType.REST_API)
                .startedAt(LocalDateTime.now())
                .triggeredBy(currentUser)
                .build();
        job = ingestionRepository.save(job);

        int maxRetries = 3;
        int currentAttempt = 0;
        long currentBackoffMs = 1000;

        while (currentAttempt <= maxRetries) {
            int processed = 0, failed = 0;

            try {
                if (currentAttempt > 0) {
                    job.setStatus(IngestionJob.JobStatus.RETRYING);
                    job.setRetryCount(currentAttempt);
                    job.setLastAttemptedAt(LocalDateTime.now());
                    ingestionRepository.save(job);
                    log.info("API Ingestion retrying (attempt {}): jobId={}", currentAttempt, job.getId());
                }

                String url = source.getConnectionDetails().get("url");
                if (url == null || url.isBlank()) {
                    throw new com.company.integrationplatform.exception.IngestionException(
                            "Data source missing 'url' in connection details");
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> apiRecords = restTemplate.getForObject(url, List.class);

                if (apiRecords != null) {
                    List<IngestionRecord> records = new ArrayList<>();
                    for (Map<String, Object> rawData : apiRecords) {
                        try {
                            Map<String, Object> transformed =
                                    transformationService.transform(rawData, dataSourceId);
                            records.add(IngestionRecord.builder()
                                    .jobId(job.getId())
                                    .dataSourceId(dataSourceId)
                                    .rawData(rawData)
                                    .transformedData(transformed)
                                    .status(IngestionRecord.RecordStatus.PROCESSED)
                                    .build());
                            processed++;
                        } catch (Exception e) {
                            records.add(IngestionRecord.builder()
                                    .jobId(job.getId())
                                    .dataSourceId(dataSourceId)
                                    .rawData(rawData)
                                    .status(IngestionRecord.RecordStatus.FAILED)
                                    .errorMessage(truncate(e.getMessage(), 1000))
                                    .build());
                            failed++;
                        }
                    }
                    recordRepository.saveAll(records);
                }

                job.setStatus(failed == 0
                        ? IngestionJob.JobStatus.COMPLETED
                        : IngestionJob.JobStatus.PARTIAL);
                job.setCompletedAt(LocalDateTime.now());
                job.setRecordsProcessed(processed);
                job.setRecordsFailed(failed);
                IngestionJob saved = ingestionRepository.save(job);

                auditService.log(Constants.ACTION_INGEST_API, currentUser,
                        saved.getStatus().name(),
                        String.format("API ingestion: jobId=%s, processed=%d, failed=%d",
                                saved.getId(), processed, failed));

                String type = saved.getStatus() == IngestionJob.JobStatus.COMPLETED ? "SUCCESS" : "WARNING";
                String title = type.equals("SUCCESS") ? "API Ingestion Completed" : "API Ingestion Partial";
        
                notificationService.createSystemNotification(
                        currentUser, type, title,
                        String.format("API ingestion for source '%s' processed. Processed: %d, Failed: %d", source.getName(), processed, failed),
                        "INGESTION_JOB", saved.getId()
                );

                return java.util.concurrent.CompletableFuture.completedFuture(IngestionDto.JobResponse.from(saved));

            } catch (Exception e) {
                boolean isRetryable = e instanceof org.springframework.web.client.RestClientException || 
                                      e.getMessage().toLowerCase().contains("timeout") ||
                                      e.getMessage().toLowerCase().contains("connection");
                                      
                if (!isRetryable || currentAttempt == maxRetries) {
                    job.setStatus(IngestionJob.JobStatus.FAILED);
                    job.setErrorMessage(truncate(e.getMessage(), 1000));
                    job.setCompletedAt(LocalDateTime.now());
                    job.setRecordsProcessed(processed);
                    job.setRecordsFailed(failed);
                    IngestionJob saved = ingestionRepository.save(job);

                    log.error("API ingestion failed permanently after {} retries: source={}, error={}", 
                              currentAttempt, dataSourceId, e.getMessage());

                    notificationService.createSystemNotification(
                            currentUser, "ERROR", "API Ingestion Failed",
                            String.format("API ingestion for source '%s' failed: %s", source.getName(), truncate(e.getMessage(), 100)),
                            "INGESTION_JOB", saved.getId()
                    );

                    return java.util.concurrent.CompletableFuture.completedFuture(IngestionDto.JobResponse.from(saved));
                }

                log.warn("API ingestion attempt {} failed for jobId={}, retrying in {}ms. Error: {}", 
                         currentAttempt, job.getId(), currentBackoffMs, e.getMessage());
                         
                try {
                    Thread.sleep(currentBackoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                
                currentAttempt++;
                currentBackoffMs *= 2;
            }
        }
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all ingestion jobs, newest first.
     */
    @Transactional(readOnly = true)
    public PageResponse<IngestionDto.JobResponse> getAllJobs(Pageable pageable) {
        Page<IngestionDto.JobResponse> page = ingestionRepository
                .findAll(pageable)
                .map(IngestionDto.JobResponse::from);
        return PageResponse.of(page);
    }

    /**
     * Returns full job details including a list of failed rows.
     *
     * @throws ResourceNotFoundException if no job exists with the given ID
     */
    @Transactional(readOnly = true)
    public IngestionDto.JobDetailResponse getJobById(UUID jobId) {
        IngestionJob job = ingestionRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("IngestionJob", jobId));

        List<IngestionRecord> failedRecords = recordRepository
                .findByJobIdAndStatus(jobId, IngestionRecord.RecordStatus.FAILED,
                        org.springframework.data.domain.PageRequest.of(0, 100))
                .getContent();

        long totalFailed = recordRepository
                .countByJobIdAndStatus(jobId, IngestionRecord.RecordStatus.FAILED);

        List<IngestionDto.FailedRowResponse> failedRows = failedRecords.stream()
                .map(IngestionDto.FailedRowResponse::from)
                .toList();

        return IngestionDto.JobDetailResponse.builder()
                .job(IngestionDto.JobResponse.from(job))
                .failedRows(failedRows)
                .totalFailedRows(totalFailed)
                .build();
    }

    /**
     * Returns paginated jobs for a specific data source.
     */
    @Transactional(readOnly = true)
    public PageResponse<IngestionDto.JobResponse> getJobsByDataSource(UUID dataSourceId,
                                                                       Pageable pageable) {
        Page<IngestionDto.JobResponse> page = ingestionRepository
                .findByDataSourceId(dataSourceId, pageable)
                .map(IngestionDto.JobResponse::from);
        return PageResponse.of(page);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates the uploaded file before any parsing begins.
     *
     * @throws CsvIngestionException on any validation failure
     */
    private void validateCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CsvIngestionException("No file provided or file is empty.");
        }

        if (file.getSize() > Constants.MAX_CSV_FILE_SIZE_BYTES) {
            throw new CsvIngestionException(String.format(
                    "File size %.2f MB exceeds the maximum allowed size of 10 MB.",
                    file.getSize() / (1024.0 * 1024.0)));
        }

        String originalFilename = file.getOriginalFilename();
        String contentType      = file.getContentType();

        boolean validExtension = originalFilename != null
                && originalFilename.toLowerCase().endsWith(Constants.CSV_EXTENSION);

        boolean validContentType = contentType != null
                && (contentType.equalsIgnoreCase(Constants.CSV_CONTENT_TYPE)
                    || contentType.equalsIgnoreCase("application/csv")
                    || contentType.equalsIgnoreCase("application/vnd.ms-excel")
                    || contentType.equalsIgnoreCase("text/plain"));

        if (!validExtension && !validContentType) {
            throw new CsvIngestionException(String.format(
                    "Invalid file type. Expected a CSV file (.csv), got: name='%s', contentType='%s'.",
                    originalFilename, contentType));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private DataSourceEntity getActiveSource(UUID id) {
        DataSourceEntity source = dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceNotFoundException(id));
        if (source.getStatus() != DataSourceEntity.SourceStatus.ACTIVE) {
            throw new CsvIngestionException(
                    "Data source is not ACTIVE: '" + source.getName()
                    + "'. Activate it before ingesting.");
        }
        return source;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}
