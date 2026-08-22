package com.ledgerflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.cache.domain.Customer;
import com.ledgerflow.cache.domain.Product;
import com.ledgerflow.cache.repository.CustomerRepository;
import com.ledgerflow.cache.repository.ProductRepository;
import com.ledgerflow.idempotency.repository.IdempotencyRecordRepository;
import com.ledgerflow.ledger.repository.LedgerEntryRepository;
import com.ledgerflow.ledger.repository.LedgerTransactionRepository;
import com.ledgerflow.messaging.repository.ProcessedEventRepository;
import com.ledgerflow.order.repository.OrderRepository;
import com.ledgerflow.outbox.repository.OutboxEventRepository;
import com.ledgerflow.payment.repository.PaymentAttemptRepository;
import com.ledgerflow.payment.repository.PaymentRepository;
import com.ledgerflow.payment.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected CustomerRepository customerRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

    @Autowired
    protected PaymentAttemptRepository attemptRepository;

    @Autowired
    protected RefundRepository refundRepository;

    @Autowired
    protected LedgerTransactionRepository ledgerTransactionRepository;

    @Autowired
    protected LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    protected IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    protected OutboxEventRepository outboxEventRepository;

    @Autowired
    protected ProcessedEventRepository processedEventRepository;

    @MockBean
    protected RabbitTemplate rabbitTemplate;

    @MockBean
    protected org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    @MockBean
    protected RedisTemplate<String, Object> redisTemplate;

    protected Customer testCustomer;
    protected Product testProduct;

    @BeforeEach
    void baseSetUp() {
        // Mock Redis rate limiter operations
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(0L);

        // Seed fresh customer & product
        customerRepository.deleteAll();
        productRepository.deleteAll();
        orderRepository.deleteAll();
        paymentRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        ledgerTransactionRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();

        testCustomer = customerRepository.save(Customer.builder()
                .id("c-test-001")
                .name("Acme Corp")
                .email("billing@acme.test")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        testProduct = productRepository.save(Product.builder()
                .id("p-test-001")
                .name("Cloud Enterprise Tier 1")
                .sku("SKU-TEST-001")
                .priceCents(4999)
                .currency("EUR")
                .stockQuantity(1000)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }
}
