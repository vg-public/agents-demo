package com.epam.agents.dto.response;

import java.util.List;

/**
 * Response DTO for the bulk price update operation.
 *
 * @param updatedCount
 *            the number of products whose price was successfully updated
 * @param notFoundSkus
 *            SKUs present in the request that did not match any product in the database
 * @param invalidSkus
 *            SKUs whose {@code newPrice} was {@code null} or {@code <= 0}; these were skipped
 */
public record BulkPriceUpdateResponse(int updatedCount, List<String> notFoundSkus, List<String> invalidSkus) {
}
