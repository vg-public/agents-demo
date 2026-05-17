package com.epam.agents.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new product.
 *
 * <p>
 * All fields are validated at the controller layer using Jakarta Bean Validation.
 * </p>
 *
 * @param sku
 *            stock-keeping unit — uppercase alphanumeric with hyphens, unique per product
 * @param name
 *            human-readable product name
 * @param price
 *            unit price in the catalog; must be positive and have at most 2 decimal places
 */
public record CreateProductRequest(@NotBlank(message = "SKU is required") @Size(max = 50, message = "SKU must not exceed 50 characters") @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU must contain only uppercase letters, digits, and hyphens") String sku,

        @NotBlank(message = "Product name is required") @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters") String name,

        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than zero") @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places") BigDecimal price) {
}
