# LedgerFlow

LedgerFlow is a Java 21 payment and order infrastructure project focused on monetary precision, explicit lifecycle rules, double-entry accounting, idempotency and reliable event processing.

The repository is organized into independently verifiable layers. The first layer contains the core domain invariants:

- integer minor-unit money arithmetic with currency checks;
- explicit order and payment state machines;
- balanced double-entry ledger validation;
- deterministic request fingerprints for idempotent operations.

## Verify the core layer

```bash
mvn -Dtest=MoneyTest,OrderStateMachineTest,PaymentStateMachineTest,LedgerBalanceTest,RequestHasherTest test
```

Additional API, persistence, messaging, observability and deployment layers are integrated only after their own checks pass.

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE).
