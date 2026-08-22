package com.ledgerflow.ledger.service;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.ledger.domain.EntryType;
import com.ledgerflow.ledger.domain.LedgerEntry;
import com.ledgerflow.ledger.domain.LedgerTransaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DoubleEntryValidator {

    public void validate(LedgerTransaction transaction) {
        List<LedgerEntry> entries = transaction.getEntries();
        if (entries == null || entries.size() < 2) {
            throw new DomainException(ErrorCode.LEDGER_IMBALANCE, "A balanced ledger transaction requires at least 2 entries (one DEBIT and one CREDIT)");
        }

        String baseCurrency = entries.getFirst().getCurrency();
        long totalDebits = 0;
        long totalCredits = 0;

        for (LedgerEntry entry : entries) {
            if (entry.getAmountCents() <= 0) {
                throw new DomainException(ErrorCode.INVALID_ARGUMENT, "Ledger entry amount must be strictly positive: " + entry.getAmountCents());
            }

            if (!baseCurrency.equals(entry.getCurrency())) {
                throw new DomainException(ErrorCode.CURRENCY_MISMATCH,
                        String.format("All ledger entries in a transaction must share the same currency (%s vs %s)",
                                baseCurrency, entry.getCurrency()));
            }

            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebits = Math.addExact(totalDebits, entry.getAmountCents());
            } else if (entry.getEntryType() == EntryType.CREDIT) {
                totalCredits = Math.addExact(totalCredits, entry.getAmountCents());
            }
        }

        if (totalDebits != totalCredits) {
            throw new DomainException(ErrorCode.LEDGER_IMBALANCE,
                    String.format("Ledger transaction is unbalanced! Total Debits: %d %s, Total Credits: %d %s (Delta: %d)",
                            totalDebits, baseCurrency, totalCredits, baseCurrency, totalDebits - totalCredits));
        }
    }
}
