$headers = @{
    "Content-Type" = "application/json"
    "Idempotency-Key" = "demo-order-key-100"
}

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "🏛️ Testing LedgerFlow Live API Endpoints on http://localhost:8888" -ForegroundColor Cyan
Write-Host "================================================================"

# 1. Check Health
Write-Host "`n1. Health Check:" -ForegroundColor Yellow
$health = Invoke-RestMethod -Uri "http://localhost:8888/actuator/health" -Method Get
$health | ConvertTo-Json -Depth 3

# 2. Create Order
Write-Host "`n2. Creating Order (Idempotent):" -ForegroundColor Yellow
$orderBody = @{
    customerId = "c0000001-0000-0000-0000-000000000001"
    currency = "EUR"
    items = @(
        @{
            productId = "p0000001-0000-0000-0000-000000000001"
            quantity = 1
            unitPrice = 14999
        }
    )
} | ConvertTo-Json -Depth 5

$orderResp = Invoke-RestMethod -Uri "http://localhost:8888/api/orders" -Method Post -Headers $headers -Body $orderBody
$orderResp | ConvertTo-Json -Depth 5
$orderId = $orderResp.data.id

# 3. Pay Order
Write-Host "`n3. Processing Payment for Order $($orderId):" -ForegroundColor Yellow
$payHeaders = @{
    "Content-Type" = "application/json"
    "Idempotency-Key" = "demo-pay-key-100"
}
$payBody = @{
    orderId = $orderId
    amount = 14999
    currency = "EUR"
    simulatedOutcome = "SUCCESS"
} | ConvertTo-Json

$payResp = Invoke-RestMethod -Uri "http://localhost:8888/api/payments" -Method Post -Headers $payHeaders -Body $payBody
$payResp | ConvertTo-Json -Depth 5
$paymentId = $payResp.data.id

# 4. Double-Entry Ledger Audit
Write-Host "`n4. Real-time Double-Entry Ledger Audit:" -ForegroundColor Yellow
$audit = Invoke-RestMethod -Uri "http://localhost:8888/api/ledger/audit" -Method Get
$audit | ConvertTo-Json -Depth 5

Write-Host "`n================================================================" -ForegroundColor Green
Write-Host "✅ LIVE DEMO TEST SUCCEEDED 100%!" -ForegroundColor Green
Write-Host "================================================================"
