import http from 'k6/http';
import { check } from 'k6';

export const options = { vus: 20, duration: '1m' };

const BASE = __ENV.BASE_URL || 'http://localhost:9090';
const TOKENS = [
  'eyJhbGciOiJub25lIn0.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiJ9.',
  'Bearer invalid.token.here',
  'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYWNrZXIifQ.fakesig',
  '',
];

export default function () {
  const token = TOKENS[Math.floor(Math.random()*TOKENS.length)];
  const res = http.get(`${BASE}/api/tickets/my`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  check(res, { 'rejects bad JWT (401/403)': (r) => r.status === 401 || r.status === 403 });
}
