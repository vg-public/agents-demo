package com.example.catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

// ---------- Request DTO ----------
public record CreateProductRequest(
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must be at most 255 characters")
    String name,

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    String description,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    BigDecimal price,

    @NotBlank(message = "Category is required")
    @Size(max = 100)
    String category
) {}

// ---------- Response DTO ----------
record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String category,
    boolean inStock,
    String createdAt,
    String updatedAt
) {}

// ---------- Page wrapper ----------
record PagedResponse<T>(
    java.util.List<T> items,
    int page,
    int pageSize,
    long totalElements,
    int totalPages
) {}
