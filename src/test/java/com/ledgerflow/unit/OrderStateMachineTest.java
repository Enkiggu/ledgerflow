package com.ledgerflow.unit;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.order.domain.OrderStatus;
import com.ledgerflow.order.domain.OrderStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order State Machine Unit Tests")
class OrderStateMachineTest {

    @ParameterizedTest(name = "Valid transition from {0} to {1}")
    @CsvSource({
            "CREATED, PAYMENT_PENDING",
            "CREATED, CANCELLED",
            "PAYMENT_PENDING, PAID",
            "PAYMENT_PENDING, PAYMENT_FAILED",
            "PAYMENT_PENDING, CANCELLED",
            "PAYMENT_FAILED, PAYMENT_PENDING",
            "PAYMENT_FAILED, CANCELLED",
            "PAID, REFUNDED"
    })
    void shouldAllowValidTransitions(OrderStatus from, OrderStatus to) {
        assertDoesNotThrow(() -> OrderStateMachine.validateTransition(from, to));
    }

    @Test
    @DisplayName("Should reject invalid transition from PAID to PAYMENT_PENDING with ORDER_ALREADY_PAID")
    void shouldRejectPaidToPaymentPending() {
        DomainException ex = assertThrows(DomainException.class,
                () -> OrderStateMachine.validateTransition(OrderStatus.PAID, OrderStatus.PAYMENT_PENDING));
        assertEquals(ErrorCode.ORDER_ALREADY_PAID, ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject modification of CANCELLED order with ORDER_CANCELLED")
    void shouldRejectModificationOfCancelledOrder() {
        DomainException ex = assertThrows(DomainException.class,
                () -> OrderStateMachine.validateTransition(OrderStatus.CANCELLED, OrderStatus.PAYMENT_PENDING));
        assertEquals(ErrorCode.ORDER_CANCELLED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject arbitrary invalid transition from CREATED to PAID")
    void shouldRejectDirectCreatedToPaid() {
        DomainException ex = assertThrows(DomainException.class,
                () -> OrderStateMachine.validateTransition(OrderStatus.CREATED, OrderStatus.PAID));
        assertEquals(ErrorCode.INVALID_ORDER_STATE, ex.getErrorCode());
    }

    @Test
    @DisplayName("Self-transition should be idempotent no-op")
    void shouldAllowSelfTransition() {
        assertDoesNotThrow(() -> OrderStateMachine.validateTransition(OrderStatus.CREATED, OrderStatus.CREATED));
        assertDoesNotThrow(() -> OrderStateMachine.validateTransition(OrderStatus.PAID, OrderStatus.PAID));
    }
}
