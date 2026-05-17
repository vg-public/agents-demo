package com.epam.agents.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing product.
 *
 * <p>
 * All fields are optional — only non-null values are applied to the entity
 * via the MapStruct {@code NullValuePropertyMappingStrategy.IGNORE} strategy.
 * </p>
 *
 * @param name
 *            updated product name; when null, the existing name is preserved
 * @param price
 *            updated unit price; when null, the existing price is preserved
 */
public record UpdateProductRequest(@Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters") String name,

        @DecimalMin(value = "0.01", message = "Price must be greater than zero") @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places") BigDecimal price) {
}
