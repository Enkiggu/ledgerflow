package com.ledgerflow.ledger.service;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.common.model.Money;
import com.ledgerflow.ledger.domain.*;
import com.ledgerflow.ledger.repository.LedgerEntryRepository;
import com.ledgerflow.ledger.repository.LedgerTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final DoubleEntryValidator validator;

    public LedgerService(LedgerTransactionRepository transactionRepository,
                         LedgerEntryRepository entryRepository,
                         DoubleEntryValidator validator) {
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.validator = validator;
    }

    /**
     * Records a balanced double-entry transaction for a successful payment.
     * Transaction:
     *   DEBIT:  CUSTOMER_RECEIVABLE (+100 EUR receivable claim)
     *   CREDIT: MERCHANT_SETTLEMENT (+100 EUR merchant balance liability)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerTransaction recordPaymentTransaction(String paymentId, long amountCents, String currency, String description) {
        String txId = UUID.randomUUID().toString();

        LedgerTransaction tx = LedgerTransaction.builder()
                .id(txId)
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(paymentId)
                .description(description != null ? description : "Payment settlement for payment " + paymentId)
                .build();

        LedgerEntry debitEntry = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.CUSTOMER_RECEIVABLE)
                .entryType(EntryType.DEBIT)
                .amountCents(amountCents)
                .currency(currency)
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.MERCHANT_SETTLEMENT)
                .entryType(EntryType.CREDIT)
                .amountCents(amountCents)
                .currency(currency)
                .build();

        tx.addEntry(debitEntry);
        tx.addEntry(creditEntry);

        validator.validate(tx);

        log.info("Committing balanced ledger transaction [txId: {}, ref: PAYMENT/{}, amount: {} {}]",
                txId, paymentId, amountCents, currency);

        return transactionRepository.save(tx);
    }

    /**
     * Records a compensating double-entry transaction for a payment refund.
     * Transaction:
     *   DEBIT:  MERCHANT_SETTLEMENT (-100 EUR merchant balance deduction)
     *   CREDIT: CUSTOMER_RECEIVABLE (-100 EUR customer claim settlement)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerTransaction recordRefundTransaction(String refundId, String paymentId, long amountCents, String currency, String reason) {
        String txId = UUID.randomUUID().toString();

        LedgerTransaction tx = LedgerTransaction.builder()
                .id(txId)
                .referenceType(ReferenceType.REFUND)
                .referenceId(refundId)
                .description("Refund for payment " + paymentId + ": " + reason)
                .build();

        LedgerEntry debitEntry = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.MERCHANT_SETTLEMENT)
                .entryType(EntryType.DEBIT)
                .amountCents(amountCents)
                .currency(currency)
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .accountType(AccountType.CUSTOMER_RECEIVABLE)
                .entryType(EntryType.CREDIT)
                .amountCents(amountCents)
                .currency(currency)
                .build();

        tx.addEntry(debitEntry);
        tx.addEntry(creditEntry);

        validator.validate(tx);

        log.info("Committing balanced refund ledger transaction [txId: {}, ref: REFUND/{}, amount: {} {}]",
                txId, refundId, amountCents, currency);

        return transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public LedgerTransaction getTransactionById(String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.LEDGER_TRANSACTION_NOT_FOUND, "Ledger transaction not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<LedgerTransaction> getTransactions(ReferenceType referenceType, Pageable pageable) {
        if (referenceType != null) {
            return transactionRepository.findByReferenceType(referenceType, pageable);
        }
        return transactionRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Money getAccountBalance(AccountType accountType, String currency) {
        long balanceCents = entryRepository.calculateAccountBalance(accountType, currency);
        return Money.of(balanceCents, currency);
    }
}
