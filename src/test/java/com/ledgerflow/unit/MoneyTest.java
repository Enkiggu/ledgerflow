package com.ledgerflow.unit;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.common.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money Value Object Precision & Arithmetic Tests")
class MoneyTest {

    @Test
    @DisplayName("Should correctly represent minor units without floating-point errors")
    void shouldHandleMinorUnitsAccurately() {
        Money money = Money.of(4999, "EUR");
        assertEquals(4999, money.amountMinor());
        assertEquals("EUR", money.currency());
        assertEquals(new BigDecimal("49.99"), money.toMajorUnits());
    }

    @Test
    @DisplayName("Should add money of same currency safely")
    void shouldAddMoney() {
        Money m1 = Money.of(1050, "EUR");
        Money m2 = Money.of(450, "EUR");
        Money sum = m1.plus(m2);

        assertEquals(1500, sum.amountMinor());
        assertEquals("EUR", sum.currency());
    }

    @Test
    @DisplayName("Should subtract money of same currency safely")
    void shouldSubtractMoney() {
        Money m1 = Money.of(2000, "USD");
        Money m2 = Money.of(750, "USD");
        Money diff = m1.minus(m2);

        assertEquals(1250, diff.amountMinor());
    }

    @Test
    @DisplayName("Should reject arithmetic across different currencies with CURRENCY_MISMATCH")
    void shouldRejectCrossCurrencyArithmetic() {
        Money eur = Money.of(1000, "EUR");
        Money usd = Money.of(1000, "USD");

        DomainException ex = assertThrows(DomainException.class, () -> eur.plus(usd));
        assertEquals(ErrorCode.CURRENCY_MISMATCH, ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject invalid currency formatting")
    void shouldRejectInvalidCurrency() {
        DomainException ex = assertThrows(DomainException.class, () -> Money.of(100, "EU"));
        assertEquals(ErrorCode.INVALID_CURRENCY, ex.getErrorCode());
    }
}
