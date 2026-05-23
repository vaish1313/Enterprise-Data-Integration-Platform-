package com.company.integrationplatform.ingestion;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.datasource.DataSourceEntity;
import com.company.integrationplatform.datasource.DataSourceRepository;
import com.company.integrationplatform.exception.IngestionException;
import com.company.integrationplatform.exception.ResourceNotFoundException;
import com.company.integrationplatform.transformation.TransformationService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final IngestionRepository ingestionRepository;
    private final IngestionRecordRepository recordRepository;
    private final DataSourceRepository dataSourceRepository;
    private final TransformationService transformationService;
    private final AuditService auditService;
    private final RestTemplate restTemplate;

    @Transactional
    public IngestionDto.JobResponse ingestCsv(UUID dataSourceId, MultipartFile file) {
        DataSourceEntity source = getActiveSource(dataSourceId);
        String currentUser = currentUsername();

        IngestionJob job = IngestionJob.builder()
                .dataSourceId(dataSourceId)
                .status(IngestionJob.JobStatus.RUNNING)
                .ingestionType(IngestionJob.IngestionType.CSV)
                .startedAt(LocalDateTime.now())
                .triggeredBy(currentUser)
                .build();
        job = ingestionRepository.save(job);

        int processed = 0, failed = 0;
        List<IngestionRecord> records = new ArrayList<>();

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String[] headers = reader.readNext();
            if (headers == null) throw new IngestionException("CSV file is empty");

            String[] row;
            int rowNum = 1;
            while ((row = reader.readNext()) != null) {
                try {
                    Map<String, Object> rawData = new LinkedHashMap<>();
                    for (int i = 0; i < headers.length && i < row.length; i++) {
                        rawData.put(headers[i].trim(), row[i]);
                    }
                    Map<String, Object> transformed = transformationService.transform(rawData, dataSourceId);
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
                    log.warn("Failed to process CSV row {}: {}", rowNum, e.getMessage());
                    records.add(IngestionRecord.builder()
                            .jobId(job.getId())
                            .dataSourceId(dataSourceId)
                            .status(IngestionRecord.RecordStatus.FAILED)
                            .errorMessage(e.getMessage())
                            .sourceRowNumber(rowNum)
                            .build());
                    failed++;
                }
                rowNum++;
            }

            recordRepository.saveAll(records);
            job.setStatus(failed == 0 ? IngestionJob.JobStatus.COMPLETED : IngestionJob.JobStatus.PARTIAL);

        } catch (IOException | CsvValidationException e) {
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            log.error("CSV ingestion failed for source {}: {}", dataSourceId, e.getMessage());
        }

        job.setCompletedAt(LocalDateTime.now());
        job.setRecordsProcessed(processed);
        job.setRecordsFailed(failed);
        IngestionJob saved = ingestionRepository.save(job);

        auditService.log(Constants.ACTION_INGEST_CSV, currentUser,
                saved.getStatus().name(),
                String.format("CSV ingestion: %d processed, %d failed", processed, failed));

        return IngestionDto.JobResponse.from(saved);
    }

    @Transactional
    public IngestionDto.JobResponse ingestFromApi(UUID dataSourceId) {
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

        int processed = 0, failed = 0;

        try {
            String url = source.getConnectionDetails().get("url");
            if (url == null || url.isBlank()) {
                throw new IngestionException("Data source missing 'url' in connection details");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> apiRecords = restTemplate.getForObject(url, List.class);

            if (apiRecords != null) {
                List<IngestionRecord> records = new ArrayList<>();
                for (Map<String, Object> rawData : apiRecords) {
                    try {
                        Map<String, Object> transformed = transformationService.transform(rawData, dataSourceId);
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
                                .errorMessage(e.getMessage())
                                .build());
                        failed++;
                    }
                }
                recordRepository.saveAll(records);
            }

            job.setStatus(failed == 0 ? IngestionJob.JobStatus.COMPLETED : IngestionJob.JobStatus.PARTIAL);

        } catch (Exception e) {
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            log.error("API ingestion failed for source {}: {}", dataSourceId, e.getMessage());
        }

        job.setCompletedAt(LocalDateTime.now());
        job.setRecordsProcessed(processed);
        job.setRecordsFailed(failed);
        IngestionJob saved = ingestionRepository.save(job);

        auditService.log(Constants.ACTION_INGEST_API, currentUser,
                saved.getStatus().name(),
                String.format("API ingestion: %d processed, %d failed", processed, failed));

        return IngestionDto.JobResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public IngestionDto.JobResponse getJobById(UUID jobId) {
        return IngestionDto.JobResponse.from(
                ingestionRepository.findById(jobId)
                        .orElseThrow(() -> new ResourceNotFoundException("IngestionJob", jobId)));
    }

    @Transactional(readOnly = true)
    public List<IngestionDto.JobResponse> getJobsByDataSource(UUID dataSourceId) {
        return ingestionRepository.findByDataSourceId(dataSourceId)
                .stream().map(IngestionDto.JobResponse::from).toList();
    }

    private DataSourceEntity getActiveSource(UUID id) {
        DataSourceEntity source = dataSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DataSource", id));
        if (source.getStatus() != DataSourceEntity.SourceStatus.ACTIVE) {
            throw new IngestionException("Data source is not active: " + source.getName());
        }
        return source;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
