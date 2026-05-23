package com.epam.agents.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for a product resource.
 *
 * @param id
 *            surrogate primary key
 * @param sku
 *            stock-keeping unit identifier
 * @param name
 *            human-readable product name
 * @param price
 *            unit price
 * @param archived
 *            whether the product has been soft-archived
 * @param createdAt
 *            timestamp when the product was created
 * @param updatedAt
 *            timestamp of the last update
 */
public record ProductResponse(Long id, String sku, String name, BigDecimal price, boolean archived, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
