package com.ledgerflow.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing a monetary amount in integer minor units (e.g. cents).
 * Floating-point representation is strictly avoided for financial precision.
 */
public record Money(long amountMinor, String currency) implements Serializable, Comparable<Money> {

    public Money {
        if (currency == null || currency.trim().length() != 3) {
            throw new DomainException(ErrorCode.INVALID_CURRENCY, "Currency must be a valid 3-letter ISO-4217 code: " + currency);
        }
        currency = currency.trim().toUpperCase();
    }

    public static Money of(long amountMinor, String currency) {
        return new Money(amountMinor, currency);
    }

    public static Money ofMajor(BigDecimal amountMajor, String currency) {
        if (amountMajor == null) {
            throw new DomainException(ErrorCode.INVALID_ARGUMENT, "Amount cannot be null");
        }
        long minor = amountMajor.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        return new Money(minor, currency);
    }

    public static Money zero(String currency) {
        return new Money(0L, currency);
    }

    public Money plus(Money other) {
        validateSameCurrency(other);
        return new Money(Math.addExact(this.amountMinor, other.amountMinor), this.currency);
    }

    public Money minus(Money other) {
        validateSameCurrency(other);
        return new Money(Math.subtractExact(this.amountMinor, other.amountMinor), this.currency);
    }

    public Money multiply(long factor) {
        return new Money(Math.multiplyExact(this.amountMinor, factor), this.currency);
    }

    public boolean isPositive() {
        return this.amountMinor > 0;
    }

    public boolean isZero() {
        return this.amountMinor == 0;
    }

    public boolean isNegative() {
        return this.amountMinor < 0;
    }

    public BigDecimal toMajorUnits() {
        return BigDecimal.valueOf(this.amountMinor, 2);
    }

    private void validateSameCurrency(Money other) {
        Objects.requireNonNull(other, "Other money object cannot be null");
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(ErrorCode.CURRENCY_MISMATCH,
                    String.format("Cannot operate on different currencies: %s and %s", this.currency, other.currency));
        }
    }

    @Override
    public int compareTo(Money other) {
        validateSameCurrency(other);
        return Long.compare(this.amountMinor, other.amountMinor);
    }

    @Override
    public String toString() {
        return String.format("%d %s (%s)", amountMinor, currency, toMajorUnits().toPlainString());
    }
}
