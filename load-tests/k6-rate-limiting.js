import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '20s',
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';

export default function () {
  // Blast payments endpoint above limit (limit is 10/min) to trigger 429 Too Many Requests
  const res = http.post(
    `${BASE_URL}/api/payments`,
    JSON.stringify({
      orderId: 'o-dummy-id',
      amount: 1000,
      currency: 'EUR',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
      },
    }
  );

  check(res, {
    'rate limit response or validation response': (r) => {
      if (r.status === 429) {
        return r.headers['Retry-After'] !== undefined;
      }
      return r.status === 400 || r.status === 404;
    },
    'rate limit headers exist': (r) => {
      return r.headers['X-Ratelimit-Limit'] !== undefined || r.headers['X-RateLimit-Limit'] !== undefined;
    },
  });

  sleep(0.05);
}
