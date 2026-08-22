package com.ledgerflow.unit;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.payment.domain.PaymentStateMachine;
import com.ledgerflow.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment State Machine Unit Tests")
class PaymentStateMachineTest {

    @ParameterizedTest(name = "Valid transition from {0} to {1}")
    @CsvSource({
            "CREATED, PROCESSING",
            "CREATED, FAILED",
            "CREATED, CANCELLED",
            "PROCESSING, SUCCEEDED",
            "PROCESSING, FAILED",
            "PROCESSING, CANCELLED",
            "SUCCEEDED, REFUNDED"
    })
    void shouldAllowValidTransitions(PaymentStatus from, PaymentStatus to) {
        assertDoesNotThrow(() -> PaymentStateMachine.validateTransition(from, to));
    }

    @Test
    @DisplayName("Should reject illegal transition from FAILED to SUCCEEDED")
    void shouldRejectFailedToSucceeded() {
        DomainException ex = assertThrows(DomainException.class,
                () -> PaymentStateMachine.validateTransition(PaymentStatus.FAILED, PaymentStatus.SUCCEEDED));
        assertEquals(ErrorCode.INVALID_PAYMENT_STATE, ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject illegal transition from REFUNDED to PROCESSING")
    void shouldRejectRefundedToProcessing() {
        DomainException ex = assertThrows(DomainException.class,
                () -> PaymentStateMachine.validateTransition(PaymentStatus.REFUNDED, PaymentStatus.PROCESSING));
        assertEquals(ErrorCode.INVALID_PAYMENT_STATE, ex.getErrorCode());
    }
}
