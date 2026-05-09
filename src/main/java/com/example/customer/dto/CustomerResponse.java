package com.example.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * API response payload for a single customer. {@code null} fields are omitted from
 * JSON serialization to keep CSV-imported records compact.
 *
 * @param customerId external customer identifier (serialized as {@code customerid})
 * @param name       full customer name
 * @param country    country (sourced from CSV import or API input)
 * @param street     optional street address
 * @param zipcode    optional postal code
 * @param city       city name
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerResponse(
        @JsonProperty("customerid") String customerId,
        String name,
        String country,
        String street,
        String zipcode,
        String city
) {
}
