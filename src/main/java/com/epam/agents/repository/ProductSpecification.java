package com.epam.agents.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.epam.agents.entity.Product;

/**
 * Factory of {@link Specification} predicates for dynamic {@link Product} queries.
 *
 * <p>
 * Each method returns a {@code null}-safe specification that is a no-op
 * when the supplied filter value is absent, making it safe to compose with
 * {@link Specification#and(Specification)}.
 * </p>
 */
public final class ProductSpecification {

    private ProductSpecification() {
    }

    /**
     * Case-insensitive partial match on the product name.
     *
     * @param keyword
     *            the search term; {@code null} or blank returns a no-op spec
     * @return a {@link Specification} that filters by name containing the keyword
     */
    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            return cb.like(cb.upper(root.get("name")), "%" + keyword.toUpperCase() + "%");
        };
    }

    /**
     * Filters products with price greater than or equal to {@code minPrice}.
     *
     * @param minPrice
     *            the minimum price (inclusive); {@code null} returns a no-op spec
     * @return a {@link Specification} for the lower price bound
     */
    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    /**
     * Filters products with price less than or equal to {@code maxPrice}.
     *
     * @param maxPrice
     *            the maximum price (inclusive); {@code null} returns a no-op spec
     * @return a {@link Specification} for the upper price bound
     */
    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }
}
