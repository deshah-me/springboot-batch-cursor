package com.example.customer.service;

import com.example.customer.dto.CursorPageResponse;
import com.example.customer.dto.CustomerRequest;
import com.example.customer.dto.CustomerResponse;

public interface CustomerService {

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getCustomerByCustomerId(String customerId);

    CursorPageResponse<CustomerResponse> getAllCustomers(String afterCustomerId, int limit);

    CustomerResponse updateCustomer(String customerId, CustomerRequest request);

    void deleteCustomer(String customerId);

    void deleteAllCustomers();

    int processCsvBatch();
}
