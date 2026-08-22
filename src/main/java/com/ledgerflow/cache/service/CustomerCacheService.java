package com.ledgerflow.cache.service;

import com.ledgerflow.cache.domain.Customer;
import com.ledgerflow.cache.repository.CustomerRepository;
import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerCacheService {

    private static final Logger log = LoggerFactory.getLogger(CustomerCacheService.class);
    private final CustomerRepository customerRepository;

    public CustomerCacheService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Cacheable(value = "customers", key = "#customerId", unless = "#result == null")
    @Transactional(readOnly = true)
    public Customer getCustomerById(String customerId) {
        log.debug("Cache miss for customerId: {}. Fetching from PostgreSQL", customerId);
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new DomainException(ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found with id: " + customerId));
    }

    @CacheEvict(value = "customers", key = "#customer.id")
    @Transactional
    public Customer saveCustomer(Customer customer) {
        log.info("Saving customer and invalidating cache for customerId: {}", customer.getId());
        return customerRepository.save(customer);
    }
}
