import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '15s', target: 20 }, // Ramp up to 20 virtual users
    { duration: '30s', target: 50 }, // Sustained load
    { duration: '15s', target: 0 },  // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<250', 'p(99)<500'], // 95% of requests below 250ms
    http_req_failed: ['rate<0.01'],                 // Less than 1% failure rate
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';

export default function () {
  const payload = JSON.stringify({
    customerId: 'c0000001-0000-0000-0000-000000000001',
    currency: 'EUR',
    items: [
      {
        productId: 'p0000001-0000-0000-0000-000000000001',
        quantity: 2,
        unitPrice: 4999,
      },
    ],
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': `order-k6-${__VU}-${__ITER}-${Date.now()}`,
    },
  };

  const res = http.post(`${BASE_URL}/api/orders`, payload, params);

  check(res, {
    'status is 201': (r) => r.status === 201,
    'order has ID': (r) => {
      const json = JSON.parse(r.body);
      return json.success && json.data.id !== undefined;
    },
    'total amount is 9998': (r) => {
      const json = JSON.parse(r.body);
      return json.data.totalAmountCents === 9998;
    },
  });

  sleep(0.1);
}
