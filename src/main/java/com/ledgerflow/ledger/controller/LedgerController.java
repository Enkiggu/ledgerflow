package com.ledgerflow.ledger.controller;

import com.ledgerflow.common.model.ApiResponse;
import com.ledgerflow.common.model.Money;
import com.ledgerflow.common.model.PageResponse;
import com.ledgerflow.ledger.domain.AccountType;
import com.ledgerflow.ledger.domain.ReferenceType;
import com.ledgerflow.ledger.dto.LedgerTransactionResponse;
import com.ledgerflow.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger")
@Tag(name = "Ledger", description = "Immutable double-entry financial ledger inspection endpoints")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    @Operation(summary = "Query immutable ledger transactions with pagination")
    public ResponseEntity<ApiResponse<PageResponse<LedgerTransactionResponse>>> getTransactions(
            @RequestParam(required = false) ReferenceType referenceType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<LedgerTransactionResponse> page = PageResponse.from(
                ledgerService.getTransactions(referenceType, pageable).map(LedgerTransactionResponse::from)
        );
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/audit")
    @Operation(summary = "System-wide financial ledger audit verifying debits equal credits")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> auditLedger() {
        long totalDebits = 0;
        long totalCredits = 0;

        for (AccountType type : AccountType.values()) {
            long bal = ledgerService.getAccountBalance(type, "EUR").amountMinor();
            if (bal > 0) {
                totalDebits += bal;
            } else if (bal < 0) {
                totalCredits += Math.abs(bal);
            }
        }

        boolean balanced = (totalDebits == totalCredits);

        java.util.Map<String, Object> result = java.util.Map.of(
                "status", balanced ? "BALANCED" : "DISCREPANCY_DETECTED",
                "totalDebitsCents", totalDebits,
                "totalCreditsCents", totalCredits,
                "discrepancyCents", Math.abs(totalDebits - totalCredits),
                "currency", "EUR"
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get detailed ledger transaction by ID")
    public ResponseEntity<ApiResponse<LedgerTransactionResponse>> getTransactionById(@PathVariable String id) {
        LedgerTransactionResponse response = LedgerTransactionResponse.from(ledgerService.getTransactionById(id));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/balance")
    @Operation(summary = "Get calculated net balance for an account type and currency")
    public ResponseEntity<ApiResponse<Money>> getAccountBalance(
            @RequestParam AccountType accountType,
            @RequestParam(defaultValue = "EUR") String currency) {

        Money balance = ledgerService.getAccountBalance(accountType, currency);
        return ResponseEntity.ok(ApiResponse.ok(balance));
    }
}
