package com.company.integrationplatform.datasource;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.common.PageResponse;
import com.company.integrationplatform.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service layer for the Data Source Management module.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD operations with full validation</li>
 *   <li>Dynamic search with pagination and sorting</li>
 *   <li>Audit logging for every mutating operation</li>
 *   <li>Entity ↔ DTO mapping (entities never leave this layer)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final AuditService auditService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a new data source.
     *
     * @throws ValidationException if a data source with the same name already exists
     */
    @Transactional
    public DataSourceDto.Response create(DataSourceDto.CreateRequest request) {
        validateUniqueName(request.getName(), null);

        String currentUser = currentUsername();

        DataSourceEntity entity = DataSourceEntity.builder()
                .name(request.getName().trim())
                .sourceType(request.getSourceType())
                .connectionDetails(request.getConnectionDetails())
                .description(request.getDescription())
                .status(DataSourceEntity.SourceStatus.INACTIVE)
                .createdBy(currentUser)
                .build();

        DataSourceEntity saved = dataSourceRepository.save(entity);

        auditService.log(
                Constants.ACTION_CREATE_SOURCE,
                currentUser,
                "SUCCESS",
                String.format("Created data source: name='%s', type=%s, id=%s",
                        saved.getName(), saved.getSourceType(), saved.getId())
        );

        log.info("Data source created: id={}, name={}, type={}, user={}",
                saved.getId(), saved.getName(), saved.getSourceType(), currentUser);

        return DataSourceDto.Response.from(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the full details of a single data source by ID.
     *
     * @throws DataSourceNotFoundException if no data source exists with the given ID
     */
    @Transactional(readOnly = true)
    public DataSourceDto.Response getById(UUID id) {
        return DataSourceDto.Response.from(findById(id));
    }

    /**
     * Returns a paginated, sorted list of all data sources as lightweight summaries.
     * Connection details are excluded from list responses for security.
     *
     * @param pageable pagination and sort parameters
     * @return paginated page of {@link DataSourceDto.Summary}
     */
    @Transactional(readOnly = true)
    public PageResponse<DataSourceDto.Summary> getAll(Pageable pageable) {
        Page<DataSourceDto.Summary> page = dataSourceRepository
                .findAll(pageable)
                .map(DataSourceDto.Summary::from);
        return PageResponse.of(page);
    }

    /**
     * Searches data sources by optional name (partial match), sourceType, and status.
     * Returns lightweight summaries with pagination and sorting.
     *
     * @param criteria search filter (all fields optional)
     * @param pageable pagination and sort parameters
     * @return paginated page of matching {@link DataSourceDto.Summary}
     */
    @Transactional(readOnly = true)
    public PageResponse<DataSourceDto.Summary> search(DataSourceSearchCriteria criteria,
                                                       Pageable pageable) {
        Specification<DataSourceEntity> spec = DataSourceSpecification.withCriteria(criteria);
        Page<DataSourceDto.Summary> page = dataSourceRepository
                .findAll(spec, pageable)
                .map(DataSourceDto.Summary::from);
        return PageResponse.of(page);
    }

    /**
     * Returns all ACTIVE data sources (no pagination — used by scheduler and ingestion).
     */
    @Transactional(readOnly = true)
    public List<DataSourceDto.Summary> getActive() {
        return dataSourceRepository
                .findByStatus(DataSourceEntity.SourceStatus.ACTIVE)
                .stream()
                .map(DataSourceDto.Summary::from)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Partially updates a data source. Only non-null fields in the request are applied.
     *
     * @throws DataSourceNotFoundException if no data source exists with the given ID
     * @throws ValidationException         if the new name conflicts with an existing source
     */
    @Transactional
    public DataSourceDto.Response update(UUID id, DataSourceDto.UpdateRequest request) {
        DataSourceEntity entity = findById(id);
        String currentUser = currentUsername();

        if (request.getName() != null && !request.getName().isBlank()) {
            validateUniqueName(request.getName().trim(), id);
            entity.setName(request.getName().trim());
        }
        if (request.getConnectionDetails() != null) {
            entity.setConnectionDetails(request.getConnectionDetails());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        DataSourceEntity saved = dataSourceRepository.save(entity);

        auditService.log(
                Constants.ACTION_UPDATE_SOURCE,
                currentUser,
                "SUCCESS",
                String.format("Updated data source: name='%s', status=%s, id=%s",
                        saved.getName(), saved.getStatus(), saved.getId())
        );

        log.info("Data source updated: id={}, name={}, user={}",
                saved.getId(), saved.getName(), currentUser);

        return DataSourceDto.Response.from(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes a data source.
     *
     * @throws DataSourceNotFoundException if no data source exists with the given ID
     */
    @Transactional
    public void delete(UUID id) {
        DataSourceEntity entity = findById(id);
        String currentUser = currentUsername();

        dataSourceRepository.delete(entity);

        auditService.log(
                Constants.ACTION_DELETE_SOURCE,
                currentUser,
                "SUCCESS",
                String.format("Deleted data source: name='%s', type=%s, id=%s",
                        entity.getName(), entity.getSourceType(), entity.getId())
        );

        log.info("Data source deleted: id={}, name={}, user={}",
                entity.getId(), entity.getName(), currentUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Looks up a DataSourceEntity by ID.
     *
     * @throws DataSourceNotFoundException if not found
     */
    DataSourceEntity findById(UUID id) {
        return dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceNotFoundException(id));
    }

    /**
     * Validates that the given name is not already taken by another data source.
     *
     * @param name      the name to check
     * @param excludeId the ID to exclude from the check (null on create, set on update)
     * @throws ValidationException if the name is already in use
     */
    private void validateUniqueName(String name, UUID excludeId) {
        boolean conflict = (excludeId == null)
                ? dataSourceRepository.existsByName(name)
                : dataSourceRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);

        if (conflict) {
            throw new ValidationException(
                    "A data source with name '" + name + "' already exists");
        }
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
