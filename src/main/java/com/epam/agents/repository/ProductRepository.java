package com.epam.agents.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.epam.agents.entity.Product;

/**
 * Spring Data JPA repository for {@link Product} entities.
 *
 * <p>
 * Inherits full CRUD, pagination, and sorting support from {@link JpaRepository}.
 * {@link JpaSpecificationExecutor} enables dynamic {@code Specification}-based queries
 * used by the product search feature.
 * Custom derived query methods are defined below.
 * </p>
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Checks whether a product with the given SKU already exists.
     *
     * @param sku
     *            the SKU to check
     * @return {@code true} if a product with this SKU is present; {@code false} otherwise
     */
    boolean existsBySku(String sku);

    /**
     * Finds a product by its unique SKU.
     *
     * @param sku
     *            the SKU to search for
     * @return an {@link Optional} containing the product, or empty if not found
     */
    Optional<Product> findBySku(String sku);
}
