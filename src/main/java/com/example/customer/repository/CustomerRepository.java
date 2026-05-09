package com.example.customer.repository;

import com.example.customer.entity.Customer;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Customer} entities, keyed by the external
 * {@code customerId} string. Includes derived queries used by cursor pagination.
 */
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Returns customers whose id is strictly greater than {@code afterCustomerId}, ordered ascending.
     *
     * @param afterCustomerId exclusive lower bound cursor
     * @param limit           maximum rows to fetch
     * @return ordered slice; may be empty
     */
    List<Customer> findByCustomerIdGreaterThanOrderByCustomerIdAsc(String afterCustomerId, Limit limit);

    /**
     * Returns the first page of customers ordered by id ascending.
     *
     * @param limit maximum rows to fetch
     * @return ordered slice; may be empty
     */
    List<Customer> findAllByOrderByCustomerIdAsc(Limit limit);
}
