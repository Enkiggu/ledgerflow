package com.ledgerflow.payment.service;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.ledger.service.LedgerService;
import com.ledgerflow.messaging.event.RefundCompletedEvent;
import com.ledgerflow.order.domain.Order;
import com.ledgerflow.order.domain.OrderStatus;
import com.ledgerflow.order.repository.OrderRepository;
import com.ledgerflow.outbox.service.OutboxService;
import com.ledgerflow.payment.domain.Payment;
import com.ledgerflow.payment.domain.PaymentStatus;
import com.ledgerflow.payment.domain.Refund;
import com.ledgerflow.payment.dto.RefundRequest;
import com.ledgerflow.payment.dto.RefundResponse;
import com.ledgerflow.payment.repository.PaymentRepository;
import com.ledgerflow.payment.repository.RefundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final LedgerService ledgerService;
    private final OutboxService outboxService;

    public RefundService(PaymentRepository paymentRepository,
                         RefundRepository refundRepository,
                         OrderRepository orderRepository,
                         LedgerService ledgerService,
                         OutboxService outboxService) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.ledgerService = ledgerService;
        this.outboxService = outboxService;
    }

    @Transactional
    public RefundResponse processRefund(String paymentId, RefundRequest request) {
        log.info("Processing refund for payment: {} [amount: {} {}]", paymentId, request.amount(), request.currency());

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCEEDED && payment.getStatus() != PaymentStatus.REFUNDED) {
            throw new DomainException(ErrorCode.INVALID_PAYMENT_STATE, "Only SUCCEEDED payments can be refunded. Current status: " + payment.getStatus());
        }

        if (!payment.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new DomainException(ErrorCode.CURRENCY_MISMATCH,
                    String.format("Refund currency %s does not match payment currency %s", request.currency(), payment.getCurrency()));
        }

        // Calculate total already refunded
        List<Refund> existingRefunds = refundRepository.findByPaymentId(paymentId);
        long totalRefundedSoFar = existingRefunds.stream().mapToLong(Refund::getAmountCents).sum();
        long newTotalRefunded = Math.addExact(totalRefundedSoFar, request.amount());

        if (newTotalRefunded > payment.getAmountCents()) {
            throw new DomainException(ErrorCode.REFUND_EXCEEDS_PAYMENT,
                    String.format("Total refund amount (%d) would exceed original payment amount (%d)",
                            newTotalRefunded, payment.getAmountCents()));
        }

        String refundId = UUID.randomUUID().toString();
        Refund refund = Refund.builder()
                .id(refundId)
                .paymentId(payment.getId())
                .amountCents(request.amount())
                .currency(payment.getCurrency())
                .reason(request.reason())
                .status("COMPLETED")
                .build();

        Refund savedRefund = refundRepository.save(refund);

        // Update payment & order status if fully refunded
        if (newTotalRefunded == payment.getAmountCents()) {
            payment.transitionTo(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.transitionTo(OrderStatus.REFUNDED);
                orderRepository.save(order);
            });
        }

        // Compensating double-entry ledger entry
        ledgerService.recordRefundTransaction(
                savedRefund.getId(),
                payment.getId(),
                savedRefund.getAmountCents(),
                savedRefund.getCurrency(),
                request.reason()
        );

        // Outbox event
        RefundCompletedEvent event = new RefundCompletedEvent(
                UUID.randomUUID().toString(),
                savedRefund.getId(),
                payment.getId(),
                savedRefund.getAmountCents(),
                savedRefund.getCurrency(),
                request.reason(),
                Instant.now()
        );
        outboxService.saveEvent("REFUND", savedRefund.getId(), event);

        log.info("Refund completed successfully [refundId: {}, paymentId: {}, amount: {} {}]",
                savedRefund.getId(), payment.getId(), savedRefund.getAmountCents(), savedRefund.getCurrency());

        return RefundResponse.from(savedRefund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsForPayment(String paymentId) {
        return refundRepository.findByPaymentId(paymentId).stream()
                .map(RefundResponse::from)
                .toList();
    }
}
