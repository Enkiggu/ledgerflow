package com.ledgerflow.payment.repository;

import com.ledgerflow.payment.domain.Payment;
import com.ledgerflow.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByOrderId(String orderId);
    Optional<Payment> findByOrderIdAndStatus(String orderId, PaymentStatus status);
    Optional<Payment> findByProviderReference(String providerReference);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    Page<Payment> findByCurrency(String currency, Pageable pageable);
    Page<Payment> findByStatusAndCurrency(PaymentStatus status, String currency, Pageable pageable);
}
