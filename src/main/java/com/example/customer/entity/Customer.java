package com.example.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a customer row. The primary key {@code customerId} is
 * supplied externally (e.g., from a CSV import) rather than auto-generated.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @Column(name = "customer_id", length = 64, nullable = false, updatable = false)
    private String customerId;

    private String name;
    private String country;
    private String street;
    private String zipcode;
    private String city;
}
