package com.company.integrationplatform.transformation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransformationRepository extends JpaRepository<TransformationRule, UUID> {

    @Query("""
            SELECT r FROM TransformationRule r
            WHERE r.enabled = true
              AND (r.dataSourceId = :dataSourceId OR r.dataSourceId IS NULL)
            ORDER BY r.executionOrder ASC
            """)
    List<TransformationRule> findApplicableRules(UUID dataSourceId);

    List<TransformationRule> findByDataSourceIdAndEnabledTrue(UUID dataSourceId);

    List<TransformationRule> findByEnabledTrue();
}
