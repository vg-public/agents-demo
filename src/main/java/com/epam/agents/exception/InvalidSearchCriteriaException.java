package com.epam.agents.exception;

/**
 * Thrown when search request parameters are logically inconsistent —
 * for example when {@code minPrice} is greater than {@code maxPrice}.
 *
 * <p>
 * Mapped to HTTP 400 Bad Request by {@link GlobalExceptionHandler}.
 * </p>
 */
public class InvalidSearchCriteriaException extends BusinessException {

    private static final String ERROR_CODE = "INVALID_SEARCH_CRITERIA";

    /**
     * Constructs a new {@code InvalidSearchCriteriaException}.
     *
     * @param message
     *            human-readable description of the invalid criteria
     */
    public InvalidSearchCriteriaException(String message) {
        super(message, ERROR_CODE);
    }
}
