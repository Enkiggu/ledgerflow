-- ============================================================================
-- LedgerFlow — Post-Load Data Consistency & Invariant Verification Script
-- Run this script after executing load tests to verify all financial invariants.
-- ============================================================================

-- 1. Invariant 4 Verification: Every ledger transaction must be strictly balanced (Debits == Credits)
SELECT
    t.id AS transaction_id,
    t.reference_type,
    t.reference_id,
    SUM(CASE WHEN e.entry_type = 'DEBIT' THEN e.amount_cents ELSE 0 END) AS total_debits,
    SUM(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount_cents ELSE 0 END) AS total_credits,
    (SUM(CASE WHEN e.entry_type = 'DEBIT' THEN e.amount_cents ELSE 0 END) -
     SUM(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount_cents ELSE 0 END)) AS imbalance_delta
FROM ledger_transactions t
JOIN ledger_entries e ON t.id = e.transaction_id
GROUP BY t.id, t.reference_type, t.reference_id
HAVING SUM(CASE WHEN e.entry_type = 'DEBIT' THEN e.amount_cents ELSE 0 END) !=
       SUM(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount_cents ELSE 0 END);

-- Expected Output: 0 rows (ZERO unbalanced transactions)


-- 2. Invariant 3 Verification: Every SUCCEEDED payment must have a corresponding Ledger Transaction
SELECT
    p.id AS payment_id,
    p.order_id,
    p.amount_cents,
    p.currency,
    p.status
FROM payments p
LEFT JOIN ledger_transactions lt
    ON lt.reference_type = 'PAYMENT' AND lt.reference_id = p.id
WHERE p.status = 'SUCCEEDED' AND lt.id IS NULL;

-- Expected Output: 0 rows (ZERO missing ledger transactions)


-- 3. Invariant 2 Verification: No order must have more than 1 SUCCEEDED payment
SELECT
    order_id,
    COUNT(*) AS succeeded_payment_count
FROM payments
WHERE status = 'SUCCEEDED'
GROUP BY order_id
HAVING COUNT(*) > 1;

-- Expected Output: 0 rows (ZERO duplicate payments per order)


-- 4. Invariant 6 Verification: Every SUCCEEDED payment must have an Outbox Event
SELECT
    p.id AS payment_id,
    p.status
FROM payments p
LEFT JOIN outbox_events oe
    ON oe.aggregate_type = 'PAYMENT' AND oe.aggregate_id = p.id AND oe.event_type = 'PaymentSucceeded'
WHERE p.status = 'SUCCEEDED' AND oe.id IS NULL;

-- Expected Output: 0 rows (ZERO missing outbox events)


-- 5. Chart of Accounts Summary
SELECT
    account_type,
    currency,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount_cents ELSE -amount_cents END) AS net_balance_minor
FROM ledger_entries
GROUP BY account_type, currency
ORDER BY account_type;
