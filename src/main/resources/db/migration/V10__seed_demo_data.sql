-- V10: Seed Demo Data
INSERT INTO customers (id, name, email, status, created_at, updated_at) VALUES
('c0000001-0000-0000-0000-000000000001', 'Acme Commerce Corp', 'billing@acmecommerce.io', 'ACTIVE', NOW(), NOW()),
('c0000002-0000-0000-0000-000000000002', 'Globex Retail Ltd', 'finance@globexretail.com', 'ACTIVE', NOW(), NOW()),
('c0000003-0000-0000-0000-000000000003', 'Northstar Digital', 'payments@northstardigital.net', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO products (id, name, sku, price_cents, currency, stock_quantity, created_at, updated_at) VALUES
('p0000001-0000-0000-0000-000000000001', 'Enterprise Cloud Tier 1', 'SKU-CLOUD-T1', 4999, 'EUR', 1000, NOW(), NOW()),
('p0000002-0000-0000-0000-000000000002', 'Payment Gateway Terminal Pro', 'SKU-POS-PRO', 29900, 'EUR', 250, NOW(), NOW()),
('p0000003-0000-0000-0000-000000000003', 'Compliance & Security Suite', 'SKU-SEC-ANNUAL', 99000, 'EUR', 500, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
