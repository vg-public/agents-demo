package com.epam.agents.repository.specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.epam.agents.entity.Product;

/**
 * Dynamic database filters for product catalog queries.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /**
     * Creates a filter matching an optional name/SKU substring and inclusive price bounds.
     *
     * @param search
     *            optional substring to match against name or SKU
     * @param minPrice
     *            optional inclusive minimum price
     * @param maxPrice
     *            optional inclusive maximum price
     * @return a JPA specification containing only the supplied filters
     */
    public static Specification<Product> withFilters(String search, BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
                Predicate nameMatches = criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("name")), pattern, '\\');
                Predicate skuMatches = criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("sku")), pattern, '\\');
                predicates.add(criteriaBuilder.or(nameMatches, skuMatches));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<BigDecimal>get("price"), maxPrice));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}