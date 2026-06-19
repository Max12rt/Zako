import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const notifLatency = new Trend('notification_duration', true);

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
    http_req_failed:       ['rate<0.05'],
    notification_duration: ['p(95)<500'],
  },
};

const NOTIF = 'http://localhost:8084';

export default function () {
  // health check
  const start = Date.now();
  const health = http.get(`${NOTIF}/actuator/health`);
  notifLatency.add(Date.now() - start);
  check(health, { 'notification health UP': (r) => r.status === 200 });

  // WebSocket upgrade handshake (SockJS info endpoint)
  const info = http.get(`${NOTIF}/ws/info`);
  check(info, { 'ws info reachable': (r) => r.status === 200 || r.status === 404 });

  sleep(0.5);
}
