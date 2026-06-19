import http from 'k6/http';
import { check } from 'k6';

export const options = { vus: 30, duration: '2m' };

const BASE = __ENV.BASE_URL || 'http://localhost:9090';
const passwords = ['admin','123456','password','zako123','test','qwerty','1234'];

export default function () {
  const res = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ email: 'admin@zako.pl', password: passwords[Math.floor(Math.random()*passwords.length)] }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'blocked (401/429/403)': (r) => [401,429,403].includes(r.status) });
}
