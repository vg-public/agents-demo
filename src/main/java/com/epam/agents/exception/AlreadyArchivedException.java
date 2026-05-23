package com.epam.agents.exception;

/**
 * Thrown when an attempt is made to archive a product that has already been archived.
 *
 * <p>
 * Maps to HTTP 409 Conflict via {@link GlobalExceptionHandler}.
 * </p>
 */
public class AlreadyArchivedException extends BusinessException {

    /**
     * Constructs an {@code AlreadyArchivedException} for the given SKU.
     *
     * @param sku
     *            the SKU of the product that is already archived
     */
    public AlreadyArchivedException(String sku) {
        super(String.format("Product with sku '%s' is already archived", sku), "ALREADY_ARCHIVED");
    }
}
