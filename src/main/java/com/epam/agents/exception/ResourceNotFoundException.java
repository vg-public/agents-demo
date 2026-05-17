package com.epam.agents.exception;

/**
 * Thrown when a requested resource cannot be found in the data store.
 *
 * <p>
 * Maps to HTTP 404 Not Found via {@link GlobalExceptionHandler}.
 * </p>
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * Constructs a {@code ResourceNotFoundException} with a descriptive message.
     *
     * @param resource
     *            the entity type that was not found (e.g. "Product")
     * @param field
     *            the field used for lookup (e.g. "id")
     * @param value
     *            the value that was searched for
     */
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: %s", resource, field, value), "RESOURCE_NOT_FOUND");
    }
}
