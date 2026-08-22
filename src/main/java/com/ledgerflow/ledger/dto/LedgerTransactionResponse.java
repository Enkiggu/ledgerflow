package com.ledgerflow.ledger.dto;

import com.ledgerflow.ledger.domain.LedgerTransaction;
import com.ledgerflow.ledger.domain.ReferenceType;

import java.time.Instant;
import java.util.List;

public record LedgerTransactionResponse(
        String id,
        ReferenceType referenceType,
        String referenceId,
        String description,
        List<LedgerEntryResponse> entries,
        Instant createdAt
) {
    public static LedgerTransactionResponse from(LedgerTransaction tx) {
        return new LedgerTransactionResponse(
                tx.getId(),
                tx.getReferenceType(),
                tx.getReferenceId(),
                tx.getDescription(),
                tx.getEntries() != null
                        ? tx.getEntries().stream().map(LedgerEntryResponse::from).toList()
                        : List.of(),
                tx.getCreatedAt()
        );
    }
}
