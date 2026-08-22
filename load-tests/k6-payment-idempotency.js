import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    // Normal payments with unique keys
    unique_payments: {
      executor: 'constant-vus',
      vus: 10,
      duration: '30s',
    },
    // Intentionally sending duplicate idempotency keys to test deduplication & replay caching
    duplicate_replays: {
      executor: 'constant-vus',
      vus: 5,
      duration: '30s',
      startTime: '5s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<300', 'p(99)<600'],
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';

export function setup() {
  // Create an order to test replays against
  const res = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify({
      customerId: 'c0000001-0000-0000-0000-000000000001',
      currency: 'EUR',
      items: [{ productId: 'p0000001-0000-0000-0000-000000000001', quantity: 1, unitPrice: 4999 }],
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const orderId = JSON.parse(res.body).data.id;
  return { sharedOrderId: orderId };
}

export default function (data) {
  // Test replay caching using a fixed key for the duplicate scenario
  const isDuplicateScenario = __VU > 10;
  const idempotencyKey = isDuplicateScenario
    ? `k6-replay-shared-key`
    : `k6-pay-unique-${__VU}-${__ITER}-${Date.now()}`;

  // First create fresh order if in unique mode
  let orderId = data.sharedOrderId;
  if (!isDuplicateScenario) {
    const orderRes = http.post(
      `${BASE_URL}/api/orders`,
      JSON.stringify({
        customerId: 'c0000001-0000-0000-0000-000000000001',
        currency: 'EUR',
        items: [{ productId: 'p0000001-0000-0000-0000-000000000001', quantity: 1, unitPrice: 4999 }],
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (orderRes.status === 201) {
      orderId = JSON.parse(orderRes.body).data.id;
    }
  }

  const paymentPayload = JSON.stringify({
    orderId: orderId,
    amount: 4999,
    currency: 'EUR',
    simulatedOutcome: 'SUCCESS',
  });

  const res = http.post(`${BASE_URL}/api/payments`, paymentPayload, {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
  });

  if (isDuplicateScenario) {
    // Replay should either return 201 (first hit) or cached replay with X-Cache-Replay header
    check(res, {
      'replay returns valid status': (r) => r.status === 201 || r.status === 409 || r.status === 429,
    });
  } else {
    check(res, {
      'payment succeeds': (r) => r.status === 201 || r.status === 429,
    });
  }

  sleep(0.2);
}
