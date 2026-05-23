package com.epam.agents.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A single SKU + new-price pair within a {@link BulkPriceUpdateRequest}.
 *
 * <p>
 * {@code newPrice} is intentionally <em>not</em> annotated with {@code @DecimalMin} here.
 * Entries with a non-positive price are collected in {@code invalidSkus} by the service
 * rather than causing an HTTP 400 response.
 * </p>
 *
 * @param sku
 *            the product's stock-keeping unit identifier; must not be blank
 * @param newPrice
 *            the replacement price; must not be null (positive enforcement is in the service)
 */
public record PriceUpdateEntry(@NotBlank(message = "SKU must not be blank") String sku, @NotNull(message = "newPrice must not be null") BigDecimal newPrice) {
}
