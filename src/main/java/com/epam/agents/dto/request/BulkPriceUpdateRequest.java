package com.epam.agents.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for the bulk price update endpoint.
 *
 * <p>
 * The {@code updates} list must contain between 1 and 100 entries. Each entry is validated
 * independently via {@code @Valid}, but non-positive prices are handled by the service
 * (not by a constraint here) and reported in {@code invalidSkus}.
 * </p>
 *
 * @param updates
 *            the list of SKU + new-price pairs to apply; must have 1–100 entries
 */
public record BulkPriceUpdateRequest(@NotNull(message = "updates list must not be null") @Size(min = 1, max = 100, message = "updates list must have between 1 and 100 entries") @Valid List<PriceUpdateEntry> updates) {
}
