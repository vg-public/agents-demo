package com.epam.agents.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.epam.agents.dto.request.BulkPriceUpdateRequest;
import com.epam.agents.dto.request.CreateProductRequest;
import com.epam.agents.dto.request.UpdateProductRequest;
import com.epam.agents.dto.response.BulkPriceUpdateResponse;
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
@Validated
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
     * Searches products by optional keyword and price range filters.
     *
     * @param keyword
     *            case-insensitive partial name match; omit to match all
     * @param minPrice
     *            lower price bound (inclusive, must be &gt; 0); omit for no lower bound
     * @param maxPrice
     *            upper price bound (inclusive, must be &gt; 0); omit for no upper bound
     * @param page
     *            zero-based page index (default 0)
     * @param size
     *            page size (default 20)
     * @param sort
     *            sort expression in {@code field,direction} format (default "name,asc")
     * @return HTTP 200 with a paginated list of matching products
     */
    @GetMapping("/search")
    @Operation(summary = "Search products by keyword and price range")
    public ResponseEntity<Page<ProductResponse>> search(@RequestParam(required = false) String keyword, @RequestParam(required = false) @DecimalMin(value = "0.01", message = "minPrice must be greater than 0") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin(value = "0.01", message = "maxPrice must be greater than 0") BigDecimal maxPrice, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "name,asc") String sort) {

        String[] sortParts = sort.split(",", 2);
        String sortField = ALLOWED_SORT_FIELDS.contains(sortParts[0]) ? sortParts[0] : "name";
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        return ResponseEntity.ok(productService.search(keyword, minPrice, maxPrice, pageable));
    }

    /**
     * Retrieves a single product by its SKU.
     *
     * <p>
     * Returns archived products — explicit SKU lookup always resolves.
     * </p>
     *
     * @param sku
     *            the product's stock-keeping unit
     * @return HTTP 200 with the product, or HTTP 404 if not found
     */
    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ProductResponse> getBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getBySku(sku));
    }

    /**
     * Soft-archives a product by SKU, setting {@code archived = true}.
     *
     * @param sku
     *            the SKU of the product to archive
     * @return HTTP 200 with the updated product, HTTP 404 if not found, HTTP 409 if already archived
     */
    @PatchMapping("/sku/{sku}/archive")
    @Operation(summary = "Archive (soft-delete) a product by SKU")
    public ResponseEntity<ProductResponse> archive(@PathVariable String sku) {
        return ResponseEntity.ok(productService.archive(sku));
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

    /**
     * Updates the price of multiple products in a single transaction.
     *
     * <p>
     * Entries with a non-positive {@code newPrice} are reported in {@code invalidSkus}.
     * Entries whose SKU is not found are reported in {@code notFoundSkus}.
     * Neither condition causes HTTP 4xx — they are included in the HTTP 200 response.
     * </p>
     *
     * @param request
     *            1–100 SKU + new-price pairs
     * @return HTTP 200 with update summary
     */
    @PatchMapping("/prices")
    @Operation(summary = "Bulk update product prices by SKU")
    public ResponseEntity<BulkPriceUpdateResponse> bulkUpdatePrices(@Valid @RequestBody BulkPriceUpdateRequest request) {
        return ResponseEntity.ok(productService.bulkUpdatePrices(request));
    }
}
