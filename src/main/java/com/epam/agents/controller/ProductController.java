package com.epam.agents.controller;

import java.net.URI;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.epam.agents.dto.request.CreateProductRequest;
import com.epam.agents.dto.request.UpdateProductRequest;
import com.epam.agents.dto.response.PagedResponse;
import com.epam.agents.dto.response.ProductResponse;
import com.epam.agents.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for the product catalog resource.
 *
 * <p>
 * All endpoints are prefixed with {@code /api/v1/products}. Business logic is
 * fully delegated to {@link ProductService} — this controller is intentionally thin.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalog CRUD operations")
public class ProductController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "price", "sku", "createdAt");
    private static final String DEFAULT_SORT_FIELD = "id";

    private final ProductService productService;

    /**
     * Constructs a {@code ProductController} with its required service dependency.
     *
     * @param productService
     *            the product service
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Retrieves a single product by its surrogate ID.
     *
     * @param id
     *            the product primary key
     * @return HTTP 200 with the product, or HTTP 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * Returns a paginated, sorted list of all products.
     *
     * @param page
     *            zero-based page index (default 0)
     * @param size
     *            page size (default 20)
     * @param sortBy
     *            field to sort by; validated against allowed fields (default "id")
     * @param sortDir
     *            sort direction: {@code asc} or {@code desc} (default "asc")
     * @return HTTP 200 with a {@link PagedResponse}
     */
    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<PagedResponse<ProductResponse>> getAll(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "id") String sortBy, @RequestParam(defaultValue = "asc") String sortDir) {

        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(safeSortBy).descending() : Sort.by(safeSortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponse> results = productService.getAll(pageable);
        return ResponseEntity.ok(PagedResponse.from(results));
    }

    /**
     * Creates a new product in the catalog.
     *
     * @param request
     *            the validated creation request
     * @return HTTP 201 Created with the new product and a {@code Location} header
     */
    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates an existing product. Only non-null fields in the request body are applied.
     *
     * @param id
     *            the product's primary key
     * @param request
     *            the validated update request
     * @return HTTP 200 with the updated product, or HTTP 404 if not found
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    /**
     * Deletes a product by ID.
     *
     * @param id
     *            the product's primary key
     * @return HTTP 204 No Content on success, or HTTP 404 if not found
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
