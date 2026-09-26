package com.epam.agents.exception;

/**
 * Thrown when the minimum product price exceeds the maximum product price.
 *
 * <p>
 * Maps to HTTP 400 Bad Request via {@link GlobalExceptionHandler}.
 * </p>
 */
public class InvalidPriceRangeException extends BusinessException {

    /**
     * Constructs an {@code InvalidPriceRangeException}.
     */
    public InvalidPriceRangeException() {
        super("Minimum price must not exceed maximum price", "INVALID_PRICE_RANGE");
    }
}
