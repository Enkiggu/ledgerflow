import http from 'k6/http';
import { check } from 'k6';

export const options = {
  // 50 concurrent virtual users firing simultaneously at the exact same target order
  scenarios: {
    thundering_herd: {
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      maxDuration: '10s',
    },
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';

export function setup() {
  // Create single target order
  const orderRes = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify({
      customerId: 'c0000001-0000-0000-0000-000000000001',
      currency: 'EUR',
      items: [{ productId: 'p0000001-0000-0000-0000-000000000001', quantity: 1, unitPrice: 4999 }],
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  return { targetOrderId: JSON.parse(orderRes.body).data.id };
}

export default function (data) {
  const payload = JSON.stringify({
    orderId: data.targetOrderId,
    amount: 4999,
    currency: 'EUR',
    simulatedOutcome: 'SUCCESS',
  });

  const res = http.post(`${BASE_URL}/api/payments`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': `race-test-vu-${__VU}`,
    },
  });

  check(res, {
    'expected status (201 success or 409/429 conflict)': (r) =>
      r.status === 201 || r.status === 409 || r.status === 429,
  });
}
