package com.example.customer.exception;

/**
 * Raised by the CSV import flow when a row is malformed, a value is missing, or any
 * underlying I/O / persistence failure occurs. Triggers a full transactional rollback.
 */
public class CsvBatchProcessingException extends RuntimeException {

    /**
     * @param message detail describing the failure (often row-scoped)
     */
    public CsvBatchProcessingException(String message) {
        super(message);
    }

    /**
     * @param message detail describing the failure
     * @param cause   the underlying exception that triggered this failure
     */
    public CsvBatchProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
