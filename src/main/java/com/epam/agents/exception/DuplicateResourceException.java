package com.epam.agents.exception;

/**
 * Thrown when an attempt is made to create a resource that already exists.
 *
 * <p>
 * Maps to HTTP 409 Conflict via {@link GlobalExceptionHandler}.
 * </p>
 */
public class DuplicateResourceException extends BusinessException {

    /**
     * Constructs a {@code DuplicateResourceException} with a descriptive message.
     *
     * @param resource
     *            the entity type that already exists (e.g. "Product")
     * @param field
     *            the field whose value caused the conflict (e.g. "sku")
     * @param value
     *            the duplicate value
     */
    public DuplicateResourceException(String resource, String field, Object value) {
        super(String.format("%s already exists with %s: %s", resource, field, value), "DUPLICATE_RESOURCE");
    }
}
