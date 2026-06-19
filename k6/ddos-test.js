import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    ddos: {
      executor: 'constant-arrival-rate',
      rate: 200,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 100,
      maxVUs: 300,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.8'],
  },
};

const MONO = __ENV.BASE_URL || 'http://localhost:9090';

export default function () {
  const r = http.get(`${MONO}/api/trips`);
  check(r, { 'alive': (r) => r.status < 500 });
}
