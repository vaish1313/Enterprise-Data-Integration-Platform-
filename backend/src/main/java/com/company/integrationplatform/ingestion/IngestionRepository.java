package com.company.integrationplatform.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestionRepository extends JpaRepository<IngestionJob, UUID> {

    List<IngestionJob> findByDataSourceId(UUID dataSourceId);

    List<IngestionJob> findByStatus(IngestionJob.JobStatus status);

    long countByStatus(IngestionJob.JobStatus status);

    @Query("SELECT SUM(j.recordsProcessed) FROM IngestionJob j WHERE j.status = 'COMPLETED'")
    Long sumSuccessfulRecords();

    @Query("SELECT SUM(j.recordsFailed) FROM IngestionJob j")
    Long sumFailedRecords();

    @Query("SELECT j FROM IngestionJob j ORDER BY j.createdAt DESC")
    List<IngestionJob> findLatestJobs(org.springframework.data.domain.Pageable pageable);
}
