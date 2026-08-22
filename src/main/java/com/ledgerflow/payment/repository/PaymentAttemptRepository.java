package com.ledgerflow.payment.repository;

import com.ledgerflow.payment.domain.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, String> {
    List<PaymentAttempt> findByPaymentIdOrderByAttemptNumberAsc(String paymentId);
}
