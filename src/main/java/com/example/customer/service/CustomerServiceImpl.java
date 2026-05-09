package com.example.customer.service;

import com.example.customer.dto.CursorPageResponse;
import com.example.customer.dto.CustomerRequest;
import com.example.customer.dto.CustomerResponse;
import com.example.customer.entity.Customer;
import com.example.customer.exception.CsvBatchProcessingException;
import com.example.customer.exception.CustomerNotFoundException;
import com.example.customer.mapper.CustomerMapper;
import com.example.customer.repository.CustomerRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Default {@link CustomerService} implementation backed by JPA. Handles CRUD, cursor
 * pagination, and chunked CSV ingestion within a single transactional boundary.
 */
@Slf4j
@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final int CSV_COLUMN_COUNT = 5;
    private static final int CUSTOMER_ID_INDEX = 0;
    private static final int FIRST_NAME_INDEX = 1;
    private static final int LAST_NAME_INDEX = 2;
    private static final int CITY_INDEX = 3;
    private static final int COUNTRY_INDEX = 4;

    private static final int DEFAULT_PAGE_LIMIT = 200;

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${batch.chunk-size:500}")
    private int chunkSize;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    /**
     * Creates a new customer.
     *
     * @param request the validated customer payload; {@code customerId} must be unique
     * @return the persisted customer as a response DTO
     * @throws IllegalArgumentException if a customer with the same {@code customerId} already exists
     */
    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating customer with customerId={}", request.customerId());
        if (customerRepository.existsById(request.customerId())) {
            log.warn("Create rejected: customerId={} already exists", request.customerId());
            throw new IllegalArgumentException(
                    "Customer already exists with customerId: " + request.customerId());
        }
        Customer customer = customerMapper.toEntity(request);
        entityManager.persist(customer);
        log.info("Persisted customer with customerId={}", customer.getCustomerId());
        return customerMapper.toResponse(customer);
    }

    /**
     * Fetches a single customer by its business identifier.
     *
     * @param customerId the external customer id (primary key)
     * @return the matching customer as a response DTO
     * @throws CustomerNotFoundException if no customer exists with the given id
     */
    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCustomerId(String customerId) {
        log.debug("Looking up customer with customerId={}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found with customerId={}", customerId);
                    return new CustomerNotFoundException(
                            "Customer not found with customerId: " + customerId);
                });
        return customerMapper.toResponse(customer);
    }

    /**
     * Returns a single cursor-paginated slice of customers, ordered by {@code customerId} ascending.
     * <p>
     * Internally fetches {@code limit + 1} rows so the {@code hasMore} flag and the next
     * cursor can be computed without a separate {@code COUNT(*)} query. Bounds checking on
     * {@code limit} is the controller's responsibility (Bean Validation); this method trusts
     * the value it receives and lets SQL {@code LIMIT} naturally return fewer rows when the
     * dataset is exhausted.
     *
     * @param afterCustomerId exclusive cursor; pass {@code null} or blank for the first page
     * @param limit           page size; must be positive (caller is expected to validate)
     * @return a {@link CursorPageResponse} containing the page content and pagination metadata
     */
    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<CustomerResponse> getAllCustomers(String afterCustomerId, int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_PAGE_LIMIT : limit;
        log.debug("Fetching cursor page after='{}' limit={}", afterCustomerId, safeLimit);
        // Fetch one extra row to detect whether more pages remain.
        Limit fetchLimit = Limit.of(safeLimit + 1);

        List<Customer> rows = StringUtils.isNotBlank(afterCustomerId)
                ? customerRepository.findByCustomerIdGreaterThanOrderByCustomerIdAsc(afterCustomerId, fetchLimit)
                : customerRepository.findAllByOrderByCustomerIdAsc(fetchLimit);

        boolean hasMore = rows.size() > safeLimit;
        List<Customer> page = hasMore ? rows.subList(0, safeLimit) : rows;

        List<CustomerResponse> content = page.stream().map(customerMapper::toResponse).toList();
        String nextCursor = hasMore ? page.get(page.size() - 1).getCustomerId() : null;
        log.debug("Returning page size={} hasMore={} nextCursor={}", content.size(), hasMore, nextCursor);
        return new CursorPageResponse<>(content, nextCursor, hasMore);
    }

    /**
     * Updates an existing customer's mutable fields. The {@code customerId} itself is immutable.
     *
     * @param customerId the customer to update
     * @param request    the validated payload with new field values
     * @return the updated customer as a response DTO
     * @throws CustomerNotFoundException if no customer exists with the given id
     */
    @Override
    public CustomerResponse updateCustomer(String customerId, CustomerRequest request) {
        log.info("Updating customer with customerId={}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Update failed: customer not found with customerId={}", customerId);
                    return new CustomerNotFoundException(
                            "Customer not found with customerId: " + customerId);
                });
        customerMapper.updateEntity(customer, request);
        log.info("Updated customer with customerId={}", customerId);
        return customerMapper.toResponse(customer);
    }

    /**
     * Deletes a single customer by id.
     *
     * @param customerId the customer to delete
     * @throws CustomerNotFoundException if no customer exists with the given id
     */
    @Override
    public void deleteCustomer(String customerId) {
        log.info("Deleting customer with customerId={}", customerId);
        if (!customerRepository.existsById(customerId)) {
            log.warn("Delete failed: customer not found with customerId={}", customerId);
            throw new CustomerNotFoundException("Customer not found with customerId: " + customerId);
        }
        customerRepository.deleteById(customerId);
        log.info("Deleted customer with customerId={}", customerId);
    }

    /**
     * Removes all customers in a single bulk DELETE statement and clears the persistence
     * context. Intended for development resets and bulk re-imports.
     */
    @Override
    public void deleteAllCustomers() {
        log.warn("Deleting ALL customers from the table");
        // deleteAllInBatch issues a single DELETE statement instead of loading rows,
        // which avoids assigned-ID merge issues and is far faster for bulk cleanup.
        customerRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();
        log.info("All customers deleted");
    }

    /**
     * Imports customers from {@code customers-10000.csv} on the classpath.
     * <p>
     * Reads the file in UTF-8, persists rows in chunks of {@code batch.chunk-size}, and
     * rolls the entire transaction back if any row fails to map or persist.
     *
     * @return the total number of rows successfully inserted
     * @throws CsvBatchProcessingException if the file cannot be read, a row is malformed,
     *                                     or persistence fails
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processCsvBatch() {
        log.info("Starting CSV batch import (chunkSize={})", chunkSize);
        if (chunkSize <= 0) {
            log.error("Invalid batch.chunk-size configured: {}", chunkSize);
            throw new CsvBatchProcessingException("Invalid batch.chunk-size: " + chunkSize);
        }

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new ClassPathResource("customers-10000.csv").getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1)
                .build()) {

            int totalInserted = 0;
            int csvRowNumber = 1;
            String[] line;

            while ((line = reader.readNext()) != null) {
                csvRowNumber++;
                if (isBlankRow(line)) {
                    log.debug("Skipping blank row at line {}", csvRowNumber);
                    continue;
                }

                Customer customer = mapCustomerFromCsv(line, csvRowNumber);
                entityManager.persist(customer);
                totalInserted++;

                if (totalInserted % chunkSize == 0) {
                    log.info("Flushing chunk: {} rows inserted so far", totalInserted);
                    entityManager.flush();
                    entityManager.clear();
                }
            }

            entityManager.flush();
            entityManager.clear();
            log.info("CSV batch import completed. Total rows inserted: {}", totalInserted);
            return totalInserted;
        } catch (CsvBatchProcessingException e) {
            log.error("CSV batch processing failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("CSV batch processing failed unexpectedly", e);
            throw new CsvBatchProcessingException("CSV batch processing failed, rolling back all records", e);
        }
    }

    /**
     * Maps a single CSV row into a transient {@link Customer} entity.
     *
     * @param line      the parsed CSV fields
     * @param rowNumber 1-based source row number, used in error messages
     * @return a populated, non-persisted {@link Customer}
     * @throws CsvBatchProcessingException if the row has missing columns or required values
     */
    private Customer mapCustomerFromCsv(String[] line, int rowNumber) {
        if (line.length < CSV_COLUMN_COUNT) {
            throw new CsvBatchProcessingException("Invalid CSV format at row " + rowNumber + ": expected "
                    + CSV_COLUMN_COUNT + " columns but found " + line.length);
        }

        String customerId = requiredValue(line, CUSTOMER_ID_INDEX, "Customer Id", rowNumber);
        String firstName = requiredValue(line, FIRST_NAME_INDEX, "First Name", rowNumber);
        String lastName = requiredValue(line, LAST_NAME_INDEX, "Last Name", rowNumber);
        String city = requiredValue(line, CITY_INDEX, "City", rowNumber);
        String country = requiredValue(line, COUNTRY_INDEX, "Country", rowNumber);

        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setName(firstName + " " + lastName);
        customer.setCountry(country);
        customer.setCity(city);
        return customer;
    }

    /**
     * Reads and trims a required CSV cell, throwing if the value is missing.
     *
     * @param line       the parsed CSV fields
     * @param index      column index to read
     * @param columnName human-readable column name for error messages
     * @param rowNumber  1-based source row number
     * @return the trimmed, non-empty value
     */
    private String requiredValue(String[] line, int index, String columnName, int rowNumber) {
        String value = StringUtils.trimToEmpty(line[index]);
        if (StringUtils.isEmpty(value)) {
            throw new CsvBatchProcessingException("Missing value for column '" + columnName
                    + "' at row " + rowNumber);
        }
        return value;
    }

    /**
     * @param line the parsed CSV fields
     * @return {@code true} if every cell is {@code null} or whitespace-only
     */
    private boolean isBlankRow(String[] line) {
        return Arrays.stream(line).noneMatch(StringUtils::isNotBlank);
    }
}
