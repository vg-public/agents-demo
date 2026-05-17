package com.epam.agents.exception;

/**
 * Abstract base class for all business-layer exceptions in this application.
 *
 * <p>
 * Subclasses must supply a human-readable {@code message} and a machine-readable
 * {@code errorCode} that is exposed in RFC 7807 ProblemDetail responses.
 * </p>
 */
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructs a new {@code BusinessException}.
     *
     * @param message
     *            human-readable description of the error (must not contain PII)
     * @param errorCode
     *            machine-readable error code for API consumers
     */
    protected BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Returns the machine-readable error code.
     *
     * @return the error code string
     */
    public String getErrorCode() {
        return errorCode;
    }
}
