package com.ledgerflow.payment.service;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.ledger.service.LedgerService;
import com.ledgerflow.messaging.event.OrderPaidEvent;
import com.ledgerflow.messaging.event.PaymentFailedEvent;
import com.ledgerflow.messaging.event.PaymentSucceededEvent;
import com.ledgerflow.order.domain.Order;
import com.ledgerflow.order.domain.OrderStatus;
import com.ledgerflow.order.repository.OrderRepository;
import com.ledgerflow.outbox.service.OutboxService;
import com.ledgerflow.payment.domain.Payment;
import com.ledgerflow.payment.domain.PaymentAttempt;
import com.ledgerflow.payment.domain.PaymentStatus;
import com.ledgerflow.payment.dto.InitiatePaymentRequest;
import com.ledgerflow.payment.dto.PaymentResponse;
import com.ledgerflow.payment.provider.PaymentProvider;
import com.ledgerflow.payment.provider.ProviderChargeRequest;
import com.ledgerflow.payment.provider.ProviderChargeResponse;
import com.ledgerflow.payment.provider.ProviderStatus;
import com.ledgerflow.payment.repository.PaymentAttemptRepository;
import com.ledgerflow.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final OrderRepository orderRepository;
    private final PaymentProvider paymentProvider;
    private final LedgerService ledgerService;
    private final OutboxService outboxService;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentAttemptRepository attemptRepository,
                          OrderRepository orderRepository,
                          PaymentProvider paymentProvider,
                          LedgerService ledgerService,
                          OutboxService outboxService) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.orderRepository = orderRepository;
        this.paymentProvider = paymentProvider;
        this.ledgerService = ledgerService;
        this.outboxService = outboxService;
    }

    /**
     * Executes atomic multi-aggregate payment processing:
     * 1. Acquires pessimistic write lock on Order.
     * 2. Validates amount, currency, and non-duplicate payment invariant.
     * 3. Charges external provider via PaymentProvider SPI.
     * 4. Updates Payment -> SUCCEEDED, Order -> PAID.
     * 5. Appends immutable balanced ledger transaction.
     * 6. Appends domain events to Outbox table.
     * All 6 operations commit atomically in a single PostgreSQL transaction.
     */
    @Transactional
    public PaymentResponse processPayment(InitiatePaymentRequest request) {
        log.info("Processing payment for order: {} [amount: {} {}]",
                request.orderId(), request.amount(), request.currency());

        // 1. Lock the order row to prevent concurrent charge races
        Order order = orderRepository.findByIdForUpdate(request.orderId())
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND, "Order not found with id: " + request.orderId()));

        // Invariant checks
        if (order.getStatus() == OrderStatus.PAID) {
            throw new DomainException(ErrorCode.ORDER_ALREADY_PAID, "Order " + order.getId() + " is already PAID.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new DomainException(ErrorCode.ORDER_CANCELLED, "Order " + order.getId() + " is CANCELLED.");
        }
        if (order.getTotalAmountCents() != request.amount()) {
            throw new DomainException(ErrorCode.INVALID_ARGUMENT,
                    String.format("Payment amount %d does not match order amount %d", request.amount(), order.getTotalAmountCents()));
        }
        if (!order.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new DomainException(ErrorCode.CURRENCY_MISMATCH,
                    String.format("Payment currency %s does not match order currency %s", request.currency(), order.getCurrency()));
        }

        // 2. Initialize Payment aggregate
        String paymentId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(order.getId())
                .amountCents(request.amount())
                .currency(order.getCurrency())
                .status(PaymentStatus.PROCESSING)
                .provider("MOCK_GATEWAY")
                .version(0L)
                .build();

        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        // 3. Call external payment gateway
        ProviderChargeRequest providerRequest = new ProviderChargeRequest(
                paymentId,
                order.getId(),
                request.amount(),
                order.getCurrency(),
                order.getCustomerId(),
                request.simulatedOutcome()
        );

        ProviderChargeResponse providerResponse = paymentProvider.charge(providerRequest);

        // 4. Record Gateway Attempt
        PaymentAttempt attempt = PaymentAttempt.builder()
                .id(UUID.randomUUID().toString())
                .payment(payment)
                .attemptNumber(1)
                .provider("MOCK_GATEWAY")
                .status(providerResponse.status().name())
                .requestPayload(String.format("{\"orderId\":\"%s\",\"amount\":%d}", order.getId(), request.amount()))
                .responsePayload(String.format("{\"ref\":\"%s\",\"status\":\"%s\"}", providerResponse.providerReference(), providerResponse.status()))
                .errorMessage(providerResponse.errorMessage())
                .durationMs(providerResponse.latencyMs())
                .build();

        payment.addAttempt(attempt);

        if (providerResponse.status() == ProviderStatus.SUCCESS) {
            // Success Path: Commit payment, order paid, ledger entries, and outbox events
            payment.transitionTo(PaymentStatus.SUCCEEDED);
            payment.setProviderReference(providerResponse.providerReference());

            order.transitionTo(OrderStatus.PAID);
            orderRepository.save(order);

            Payment savedPayment = paymentRepository.save(payment);

            // Double-entry ledger settlement
            ledgerService.recordPaymentTransaction(
                    savedPayment.getId(),
                    savedPayment.getAmountCents(),
                    savedPayment.getCurrency(),
                    "Payment charge for order " + order.getId()
            );

            // Outbox events
            PaymentSucceededEvent paymentEvent = new PaymentSucceededEvent(
                    UUID.randomUUID().toString(),
                    savedPayment.getId(),
                    order.getId(),
                    savedPayment.getAmountCents(),
                    savedPayment.getCurrency(),
                    savedPayment.getProviderReference(),
                    Instant.now()
            );
            outboxService.saveEvent("PAYMENT", savedPayment.getId(), paymentEvent);

            OrderPaidEvent orderEvent = new OrderPaidEvent(
                    UUID.randomUUID().toString(),
                    order.getId(),
                    savedPayment.getId(),
                    order.getCustomerId(),
                    savedPayment.getAmountCents(),
                    savedPayment.getCurrency(),
                    Instant.now()
            );
            outboxService.saveEvent("ORDER", order.getId(), orderEvent);

            log.info("Payment succeeded and committed atomically [paymentId: {}, orderId: {}, ref: {}]",
                    savedPayment.getId(), order.getId(), savedPayment.getProviderReference());

            return PaymentResponse.from(savedPayment);
        } else {
            // Failure / Declined Path
            payment.transitionTo(PaymentStatus.FAILED);
            payment.setFailureReason(providerResponse.errorMessage());

            order.transitionTo(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);

            Payment savedPayment = paymentRepository.save(payment);

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    UUID.randomUUID().toString(),
                    savedPayment.getId(),
                    order.getId(),
                    savedPayment.getAmountCents(),
                    savedPayment.getCurrency(),
                    providerResponse.errorMessage(),
                    Instant.now()
            );
            outboxService.saveEvent("PAYMENT", savedPayment.getId(), failedEvent);

            log.warn("Payment declined by provider [paymentId: {}, reason: {}]",
                    savedPayment.getId(), providerResponse.errorMessage());

            throw new DomainException(ErrorCode.PAYMENT_DECLINED,
                    "Payment was declined: " + providerResponse.errorMessage());
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found: " + paymentId));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForOrder(String orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(PaymentStatus status, String currency, Pageable pageable) {
        Page<Payment> page;
        if (status != null && currency != null) {
            page = paymentRepository.findByStatusAndCurrency(status, currency, pageable);
        } else if (status != null) {
            page = paymentRepository.findByStatus(status, pageable);
        } else if (currency != null) {
            page = paymentRepository.findByCurrency(currency, pageable);
        } else {
            page = paymentRepository.findAll(pageable);
        }
        return page.map(PaymentResponse::from);
    }
}
