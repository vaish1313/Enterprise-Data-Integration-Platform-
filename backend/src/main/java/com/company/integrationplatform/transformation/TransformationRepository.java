package com.company.integrationplatform.transformation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransformationRepository extends JpaRepository<TransformationRule, UUID> {

    // ── Active rule lookup (used by the transformation engine) ────────────────

    /**
     * Returns all active rules applicable to a given data source,
     * including global rules (dataSourceId IS NULL), ordered by executionOrder ASC.
     */
    @Query("""
            SELECT r FROM TransformationRule r
            WHERE r.active = true
              AND (r.dataSourceId = :dataSourceId OR r.dataSourceId IS NULL)
            ORDER BY r.executionOrder ASC, r.createdAt ASC
            """)
    List<TransformationRule> findActiveRulesForDataSource(UUID dataSourceId);

    /** Returns all globally active rules (no data source scope), ordered by executionOrder. */
    @Query("SELECT r FROM TransformationRule r WHERE r.active = true AND r.dataSourceId IS NULL ORDER BY r.executionOrder ASC")
    List<TransformationRule> findGlobalActiveRules();

    // ── Paginated queries (used by the API) ───────────────────────────────────

    Page<TransformationRule> findByDataSourceId(UUID dataSourceId, Pageable pageable);

    Page<TransformationRule> findByActive(boolean active, Pageable pageable);

    Page<TransformationRule> findByTransformationType(
            TransformationRule.TransformationType type, Pageable pageable);

    // ── Validation helpers ────────────────────────────────────────────────────

    boolean existsByNameAndDataSourceId(String name, UUID dataSourceId);

    boolean existsByNameAndDataSourceIdAndIdNot(String name, UUID dataSourceId, UUID id);

    long countByActiveTrue();
}
