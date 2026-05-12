import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE = __ENV.BASE_URL || 'http://zako.localhost';

export default function () {
  const health = http.get(`${BASE}/actuator/health`);
  check(health, { 'health UP': (r) => r.status === 200 });

  const trips = http.get(`${BASE}/api/trips`);
  check(trips, { 'trips 200': (r) => r.status === 200 });

  const stations = http.get(`${BASE}/api/stations`);
  check(stations, { 'stations 200': (r) => r.status === 200 });

  sleep(1);
}
