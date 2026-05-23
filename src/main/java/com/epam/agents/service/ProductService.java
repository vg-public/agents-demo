package com.epam.agents.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.epam.agents.dto.request.BulkPriceUpdateRequest;
import com.epam.agents.dto.request.CreateProductRequest;
import com.epam.agents.dto.request.UpdateProductRequest;
import com.epam.agents.dto.response.BulkPriceUpdateResponse;
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
     * Searches products by optional keyword and price range filters.
     *
     * @param keyword
     *            case-insensitive partial match on product name; {@code null} matches all
     * @param minPrice
     *            lower price bound (inclusive); {@code null} means no lower bound
     * @param maxPrice
     *            upper price bound (inclusive); {@code null} means no upper bound
     * @param pageable
     *            pagination and sorting parameters
     * @return a {@link Page} of matching {@link ProductResponse} DTOs; never {@code null}
     * @throws com.epam.agents.exception.InvalidSearchCriteriaException
     *             if {@code minPrice} is greater than {@code maxPrice}
     */
    Page<ProductResponse> search(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

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

    /**
     * Updates the price of multiple products in a single transaction.
     *
     * <p>
     * Entries with a non-positive {@code newPrice} are collected in {@code invalidSkus}.
     * Entries whose SKU is not found are collected in {@code notFoundSkus}.
     * Neither condition causes an exception — they are reported in the response.
     * </p>
     *
     * @param request
     *            the validated bulk update request (1–100 entries)
     * @return a summary with updated count, not-found SKUs, and invalid SKUs
     */
    BulkPriceUpdateResponse bulkUpdatePrices(BulkPriceUpdateRequest request);
}
