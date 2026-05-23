package com.company.integrationplatform.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestionRecordRepository extends JpaRepository<IngestionRecord, UUID> {

    List<IngestionRecord> findByJobId(UUID jobId);

    long countByJobIdAndStatus(UUID jobId, IngestionRecord.RecordStatus status);
}
