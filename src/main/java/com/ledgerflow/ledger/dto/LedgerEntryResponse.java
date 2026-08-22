package com.ledgerflow.ledger.dto;

import com.ledgerflow.ledger.domain.AccountType;
import com.ledgerflow.ledger.domain.EntryType;
import com.ledgerflow.ledger.domain.LedgerEntry;

import java.time.Instant;

public record LedgerEntryResponse(
        String id,
        AccountType accountType,
        EntryType entryType,
        long amountCents,
        String currency,
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getAccountType(),
                entry.getEntryType(),
                entry.getAmountCents(),
                entry.getCurrency(),
                entry.getCreatedAt()
        );
    }
}
