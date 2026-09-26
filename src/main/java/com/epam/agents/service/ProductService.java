package com.epam.agents.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.epam.agents.dto.request.CreateProductRequest;
import com.epam.agents.dto.request.UpdateProductRequest;
import com.epam.agents.dto.response.ProductResponse;

/**
 * Service interface for product catalog operations.
 *
 * <p>
 * Implementations must enforce all business invariants (e.g. SKU uniqueness)
 * and must never expose JPA entities — all return values use {@link ProductResponse}.
 * </p>
 */
public interface ProductService {

    /**
     * Retrieves a single product by its SKU.
     *
     * @param sku
     *            the product's stock-keeping unit
     * @return the product as a response DTO
     * @throws com.epam.agents.exception.ResourceNotFoundException
     *             if no product with the given SKU exists
     */
    ProductResponse getBySku(String sku);

    /**
     * Retrieves a single product by its surrogate ID.
     *
     * @param id
     *            the product's primary key
     * @return the product as a response DTO
     * @throws com.epam.agents.exception.ResourceNotFoundException
     *             if no product with the given ID exists
     */
    ProductResponse getById(Long id);

    /**
     * Returns a paginated list of all products.
     *
     * @param pageable
     *            pagination and sorting parameters
     * @return a {@link Page} of {@link ProductResponse} DTOs
     */
    Page<ProductResponse> getAll(Pageable pageable);

    /**
     * Searches products by name or SKU and filters by an optional inclusive price range.
     *
     * @param search
     *            optional substring to match against name or SKU
     * @param minPrice
     *            optional inclusive minimum price
     * @param maxPrice
     *            optional inclusive maximum price
     * @param pageable
     *            pagination and sorting parameters
     * @return a page of matching product response DTOs
     * @throws com.epam.agents.exception.InvalidPriceRangeException
     *             if both price bounds are supplied and the minimum exceeds the maximum
     */
    Page<ProductResponse> search(String search, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Creates a new product.
     *
     * @param request
     *            the validated creation request
     * @return the newly created product as a response DTO
     * @throws com.epam.agents.exception.DuplicateResourceException
     *             if a product with the same SKU already exists
     */
    ProductResponse create(CreateProductRequest request);

    /**
     * Updates an existing product. Only non-null fields in the request are applied.
     *
     * @param id
     *            the ID of the product to update
     * @param request
     *            the validated update request
     * @return the updated product as a response DTO
     * @throws com.epam.agents.exception.ResourceNotFoundException
     *             if no product with the given ID exists
     */
    ProductResponse update(Long id, UpdateProductRequest request);

    /**
     * Deletes an existing product by ID.
     *
     * @param id
     *            the ID of the product to delete
     * @throws com.epam.agents.exception.ResourceNotFoundException
     *             if no product with the given ID exists
     */
    void delete(Long id);
}
