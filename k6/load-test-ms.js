import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const tripLatency = new Trend('trip_search_duration', true);
const authLatency = new Trend('auth_duration', true);
const errorRate   = new Rate('errors');

export const options = {
  stages: [
    { duration: '1m',  target: 10  },
    { duration: '3m',  target: 10  },
    { duration: '2m',  target: 50  },
    { duration: '1m',  target: 100 },
    { duration: '2m',  target: 10  },
    { duration: '1m',  target: 0   },
  ],
  thresholds: {
    http_req_failed:      ['rate<0.05'],
    http_req_duration:    ['p(95)<1000', 'p(99)<2000'],
    trip_search_duration: ['p(95)<800'],
    auth_duration:        ['p(95)<600'],
  },
};

const AUTH  = 'http://localhost:8081';
const TRIP  = 'http://localhost:8082';
const TICKET = 'http://localhost:8083';

const TEST_USER = {
  email: 'k6test@zako.local',
  password: 'K6testPass1!',
};

function register() {
  const res = http.post(`${AUTH}/api/users/register`, JSON.stringify({
    email: TEST_USER.email,
    password: TEST_USER.password,
    firstName: 'K6',
    lastName: 'Test',
  }), { headers: { 'Content-Type': 'application/json' } });
  return res.status === 200 || res.status === 409;
}

function login() {
  const start = Date.now();
  const res = http.post(`${AUTH}/api/auth/login`, JSON.stringify(TEST_USER), {
    headers: { 'Content-Type': 'application/json' },
  });
  authLatency.add(Date.now() - start);
  if (res.status !== 200) return null;
  return res.json('token');
}

export function setup() {
  register();
}

export default function () {
  const start = Date.now();
  const trips = http.get(`${TRIP}/api/trips`);
  tripLatency.add(Date.now() - start);
  const ok = check(trips, { 'trips 200': (r) => r.status === 200 });
  errorRate.add(!ok);

  sleep(0.5);

  const token = login();
  if (!token) { errorRate.add(1); return; }

  const headers = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  };

  const tickets = http.get(`${TICKET}/api/tickets/my`, headers);
  check(tickets, { 'my-tickets 200': (r) => r.status === 200 });

  sleep(1);
}
