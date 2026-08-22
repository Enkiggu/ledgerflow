package com.ledgerflow.config;

import com.ledgerflow.cache.domain.Customer;
import com.ledgerflow.cache.domain.Product;
import com.ledgerflow.cache.repository.CustomerRepository;
import com.ledgerflow.cache.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public DevDataInitializer(CustomerRepository customerRepository, ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Seeding initial demo customers and products for dev profile...");

        Customer c1 = Customer.builder()
                .id("c0000001-0000-0000-0000-000000000001")
                .name("Acme Commerce Corp")
                .email("billing@acmecommerce.io")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Customer c2 = Customer.builder()
                .id("c0000002-0000-0000-0000-000000000002")
                .name("Nova Fintech Ltd")
                .email("finance@novafintech.co")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        customerRepository.save(c1);
        customerRepository.save(c2);

        Product p1 = Product.builder()
                .id("p0000001-0000-0000-0000-000000000001")
                .sku("PROD-ENTERPRISE-01")
                .name("Enterprise Cloud API Subscription")
                .priceCents(14999L)
                .currency("EUR")
                .stockQuantity(1000)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Product p2 = Product.builder()
                .id("p0000002-0000-0000-0000-000000000002")
                .sku("PROD-GATEWAY-MODULE")
                .name("Payment Gateway Add-on Module")
                .priceCents(4999L)
                .currency("EUR")
                .stockQuantity(1000)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Product p3 = Product.builder()
                .id("p0000003-0000-0000-0000-000000000003")
                .sku("PROD-AUDIT-TOOLKIT")
                .name("Real-time Ledger Audit Toolkit")
                .priceCents(9999L)
                .currency("EUR")
                .stockQuantity(1000)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        productRepository.save(p1);
        productRepository.save(p2);
        productRepository.save(p3);

        log.info("================================================================================");
        log.info("LedgerFlow Dev Profile Ready!");
        log.info("Swagger OpenAPI UI: http://localhost:8080/swagger-ui.html");
        log.info("Prometheus Actuator: http://localhost:8080/actuator/prometheus");
        log.info("Demo Customer ID: c0000001-0000-0000-0000-000000000001");
        log.info("Demo Product ID: p0000001-0000-0000-0000-000000000001");
        log.info("================================================================================");
    }
}
