package com.example.customer.mapper;

import com.example.customer.dto.CustomerRequest;
import com.example.customer.dto.CustomerResponse;
import com.example.customer.entity.Customer;
import org.springframework.stereotype.Component;

/**
 * Stateless mapper between {@link CustomerRequest}, {@link Customer} entity, and
 * {@link CustomerResponse} DTO. Kept as a Spring bean so it can be unit-tested and
 * swapped if needed.
 */
@Component
public class CustomerMapper {

    /**
     * Builds a new {@link Customer} entity from an incoming request payload.
     *
     * @param request validated input from the API layer
     * @return a transient (non-persisted) {@link Customer}
     */
    public Customer toEntity(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setCustomerId(request.customerId());
        customer.setName(request.name());
        customer.setCountry(request.country());
        customer.setStreet(request.street());
        customer.setZipcode(request.zipcode());
        customer.setCity(request.city());
        return customer;
    }

    /**
     * Copies mutable fields from the request onto an existing managed entity.
     * The {@code customerId} primary key is intentionally left untouched.
     *
     * @param customer the managed entity to mutate
     * @param request  the new field values
     */
    public void updateEntity(Customer customer, CustomerRequest request) {
        // customerId is the primary key and not modifiable here
        customer.setName(request.name());
        customer.setCountry(request.country());
        customer.setStreet(request.street());
        customer.setZipcode(request.zipcode());
        customer.setCity(request.city());
    }

    /**
     * Projects a persistence entity into the API response DTO.
     *
     * @param customer the source entity
     * @return the response DTO suitable for JSON serialization
     */
    public CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .name(customer.getName())
                .country(customer.getCountry())
                .street(customer.getStreet())
                .zipcode(customer.getZipcode())
                .city(customer.getCity())
                .build();
    }
}
