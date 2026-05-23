package com.company.integrationplatform.synchronization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncRepository extends JpaRepository<SyncJob, UUID> {

    List<SyncJob> findByDataSourceId(UUID dataSourceId);

    List<SyncJob> findByStatus(SyncJob.SyncStatus status);

    @Query("SELECT j FROM SyncJob j ORDER BY j.startTime DESC")
    List<SyncJob> findLatestJobs(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT j FROM SyncJob j WHERE j.status = 'COMPLETED' ORDER BY j.endTime DESC")
    Optional<SyncJob> findLastSuccessfulSync();

    long countByStatus(SyncJob.SyncStatus status);
}
