package com.ledgerflow.unit;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.ledger.domain.*;
import com.ledgerflow.ledger.service.DoubleEntryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Double-Entry Ledger Balancing Invariant Tests")
class LedgerBalanceTest {

    private DoubleEntryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DoubleEntryValidator();
    }

    @Test
    @DisplayName("Invariant 4: Balanced transaction (Debits == Credits) must pass validation")
    void shouldPassWhenDebitsEqualCredits() {
        LedgerTransaction tx = LedgerTransaction.builder()
                .id(UUID.randomUUID().toString())
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(UUID.randomUUID().toString())
                .description("Test Balanced Payment")
                .build();

        LedgerEntry debit = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.CUSTOMER_RECEIVABLE)
                .entryType(EntryType.DEBIT)
                .amountCents(5000)
                .currency("EUR")
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.MERCHANT_SETTLEMENT)
                .entryType(EntryType.CREDIT)
                .amountCents(5000)
                .currency("EUR")
                .build();

        tx.addEntry(debit);
        tx.addEntry(credit);

        assertDoesNotThrow(() -> validator.validate(tx));
    }

    @Test
    @DisplayName("Invariant 4: Unbalanced transaction must be rejected with LEDGER_IMBALANCE")
    void shouldThrowWhenUnbalanced() {
        LedgerTransaction tx = LedgerTransaction.builder()
                .id(UUID.randomUUID().toString())
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(UUID.randomUUID().toString())
                .description("Test Unbalanced Payment")
                .build();

        LedgerEntry debit = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.CUSTOMER_RECEIVABLE)
                .entryType(EntryType.DEBIT)
                .amountCents(5000)
                .currency("EUR")
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.MERCHANT_SETTLEMENT)
                .entryType(EntryType.CREDIT)
                .amountCents(4999) // 1 cent discrepancy!
                .currency("EUR")
                .build();

        tx.addEntry(debit);
        tx.addEntry(credit);

        DomainException ex = assertThrows(DomainException.class, () -> validator.validate(tx));
        assertEquals(ErrorCode.LEDGER_IMBALANCE, ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject ledger entries with mismatched currencies")
    void shouldThrowOnCurrencyMismatch() {
        LedgerTransaction tx = LedgerTransaction.builder()
                .id(UUID.randomUUID().toString())
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(UUID.randomUUID().toString())
                .description("Test Currency Mismatch")
                .build();

        LedgerEntry debit = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.CUSTOMER_RECEIVABLE)
                .entryType(EntryType.DEBIT)
                .amountCents(5000)
                .currency("EUR")
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.MERCHANT_SETTLEMENT)
                .entryType(EntryType.CREDIT)
                .amountCents(5000)
                .currency("USD") // Currency mismatch!
                .build();

        tx.addEntry(debit);
        tx.addEntry(credit);

        DomainException ex = assertThrows(DomainException.class, () -> validator.validate(tx));
        assertEquals(ErrorCode.CURRENCY_MISMATCH, ex.getErrorCode());
    }
}
