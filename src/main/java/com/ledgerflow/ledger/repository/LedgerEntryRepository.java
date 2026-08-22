package com.ledgerflow.ledger.repository;

import com.ledgerflow.ledger.domain.AccountType;
import com.ledgerflow.ledger.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, String> {
    List<LedgerEntry> findByTransactionId(String transactionId);
    Page<LedgerEntry> findByAccountType(AccountType accountType, Pageable pageable);

    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN e.entryType = 'DEBIT' THEN e.amountCents ELSE -e.amountCents END),
            0
        )
        FROM LedgerEntry e
        WHERE e.accountType = :accountType AND e.currency = :currency
        """)
    long calculateAccountBalance(
            @Param("accountType") AccountType accountType,
            @Param("currency") String currency
    );
}
