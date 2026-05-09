package com.example.customer.controller;

import com.example.customer.dto.CursorPageResponse;
import com.example.customer.dto.CustomerRequest;
import com.example.customer.dto.CustomerResponse;
import com.example.customer.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing CRUD and bulk-import endpoints for {@link CustomerResponse} resources.
 * <p>
 * All routes are rooted at {@code /api/customers} and operate on the externally supplied
 * {@code customerId} (string) as the resource identifier.
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Creates a new customer.
     *
     * @param request the validated customer payload
     * @return HTTP 201 with the persisted customer representation
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        log.info("Received request to create customer with customerId={}", request.customerId());
        CustomerResponse response = customerService.createCustomer(request);
        log.info("Customer created successfully with customerId={}", response.customerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetches a single customer by its external {@code customerId}.
     *
     * @param customerId the external customer identifier
     * @return HTTP 200 with the customer representation
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerByCustomerId(@PathVariable String customerId) {
        log.info("Fetching customer with customerId={}", customerId);
        CustomerResponse response = customerService.getCustomerByCustomerId(customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a cursor-paginated slice of customers ordered by {@code customerId} ascending.
     * <p>
     * Pagination is intentionally cursor-based: the client walks the dataset by passing
     * the previous response's {@code nextCursor} back as {@code after}, looping until
     * {@code hasMore} is {@code false}. The page {@code limit} is bounded by Bean Validation
     * (1–1000) so the server always returns a predictable, memory-safe payload.
     *
     * @param afterCursor exclusive cursor; pass {@code nextCursor} from the previous response, or omit for the first page
     * @param limit       page size, between 1 and 1000 (default 200)
     * @return HTTP 200 with the page slice and a {@code nextCursor} for continuation
     */
    @GetMapping
    public ResponseEntity<CursorPageResponse<CustomerResponse>> getAllCustomers(
            @RequestParam(value = "after", required = false) String afterCursor,
            @RequestParam(value = "limit", defaultValue = "200")
            @Min(value = 1, message = "limit must be at least 1")
            @Max(value = 10000, message = "limit must not exceed 1000") int limit) {
        log.info("Fetching customers page after='{}' limit={}", afterCursor, limit);
        CursorPageResponse<CustomerResponse> page = customerService.getAllCustomers(afterCursor, limit);
        log.info("Returning {} customers (hasMore={})", page.content().size(), page.hasMore());
        return ResponseEntity.ok(page);
    }

    /**
     * Updates an existing customer's mutable fields. The {@code customerId} itself is immutable.
     *
     * @param customerId the external customer identifier of the target row
     * @param request    the validated payload of new field values
     * @return HTTP 200 with the updated representation
     */
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable String customerId,
                                                           @Valid @RequestBody CustomerRequest request) {
        log.info("Updating customer with customerId={}", customerId);
        CustomerResponse response = customerService.updateCustomer(customerId, request);
        log.info("Customer updated successfully with customerId={}", customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a single customer by its external {@code customerId}.
     *
     * @param customerId the external customer identifier
     * @return HTTP 204 on success
     */
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String customerId) {
        log.info("Deleting customer with customerId={}", customerId);
        customerService.deleteCustomer(customerId);
        log.info("Customer deleted with customerId={}", customerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes every customer in the table.
     *
     * @return HTTP 204 on success
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllCustomers() {
        log.warn("Deleting ALL customers from the table");
        customerService.deleteAllCustomers();
        log.info("All customers deleted");
        return ResponseEntity.noContent().build();
    }

    /**
     * Imports customers from the bundled CSV resource in chunked batches inside a single transaction.
     *
     * @return HTTP 200 with a human-readable success message including the inserted row count
     */
    @PostMapping("/batch/csv")
    public ResponseEntity<String> processCsvBatch() {
        log.info("Starting CSV batch import");
        int count = customerService.processCsvBatch();
        log.info("CSV batch import finished. Inserted {} customers", count);
        return ResponseEntity.ok("Successfully inserted " + count + " customers from CSV");
    }
}
