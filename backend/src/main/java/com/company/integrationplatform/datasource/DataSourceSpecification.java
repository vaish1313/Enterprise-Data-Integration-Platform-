package com.company.integrationplatform.datasource;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification factory for dynamic DataSource search queries.
 * Builds a compound WHERE clause from optional filter criteria.
 * Each criterion is only applied when the corresponding field is non-null.
 */
public class DataSourceSpecification {

    private DataSourceSpecification() {}

    /**
     * Builds a {@link Specification} from the given search criteria.
     *
     * <ul>
     *   <li>{@code name}       — case-insensitive LIKE match (contains)</li>
     *   <li>{@code sourceType} — exact enum match</li>
     *   <li>{@code status}     — exact enum match</li>
     * </ul>
     *
     * @param criteria the search filter object
     * @return a Specification that can be passed to any JpaRepository method
     */
    public static Specification<DataSourceEntity> withCriteria(DataSourceSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getName() != null && !criteria.getName().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + criteria.getName().toLowerCase() + "%"
                ));
            }

            if (criteria.getSourceType() != null) {
                predicates.add(cb.equal(root.get("sourceType"), criteria.getSourceType()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
