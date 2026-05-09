package com.example.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated request payload for creating or updating a customer.
 *
 * @param customerId external customer identifier (serialized as {@code customerid}); 1–64 chars
 * @param name       full customer name; required
 * @param country    country; required
 * @param street     street address; required
 * @param zipcode    postal code; required
 * @param city       city; required
 */
public record CustomerRequest(
        @JsonProperty("customerid")
        @NotBlank(message = "customerid is required")
        @Size(max = 64, message = "customerid must be at most 64 characters")
        String customerId,
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Country is required") String country,
        @NotBlank(message = "Street is required") String street,
        @NotBlank(message = "Zipcode is required") String zipcode,
        @NotBlank(message = "City is required") String city
) {
}
