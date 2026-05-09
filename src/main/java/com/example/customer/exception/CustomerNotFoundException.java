package com.example.customer.exception;

/**
 * Thrown when no customer matches the supplied identifier. Mapped to HTTP 404.
 */
public class CustomerNotFoundException extends RuntimeException {

    /**
     * @param message detail describing which lookup failed
     */
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
