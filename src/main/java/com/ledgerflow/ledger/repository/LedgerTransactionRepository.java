package com.ledgerflow.ledger.repository;

import com.ledgerflow.ledger.domain.LedgerTransaction;
import com.ledgerflow.ledger.domain.ReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, String> {
    Optional<LedgerTransaction> findByReferenceTypeAndReferenceId(ReferenceType referenceType, String referenceId);
    Page<LedgerTransaction> findByReferenceType(ReferenceType referenceType, Pageable pageable);
}
