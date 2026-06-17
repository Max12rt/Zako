# Notification Service — Design Spec
**Date:** 2026-06-16  
**Stack:** Spring Boot 4 + Spring WebSocket (STOMP) + Kafka consumer + Angular 21

---

## Goal

Add a `notification-service` to the microservices stack that delivers real-time in-browser toast notifications when a user's ticket is purchased or cancelled. Demonstrates: Kafka consumer → WebSocket push pattern, STOMP user destinations, JWT handshake auth.

---

## Architecture

```
ticket-service  ──kafka: ticket.purchased──►  notification-service  ──STOMP /user/{id}/queue/notifications──►  Angular
                ──kafka: ticket.cancelled──►                                                                     (toast)
```

No database. Stateless Spring Boot service. WebSocket sessions held in-memory.

---

## Changes per component

### 1. TicketEvent enrichment (ticket-service + trip-service)

Current `TicketEvent(String type, Long ticketId, Long tripId, int seats)` lacks user context. Add:

```java
record TicketEvent(String type, Long ticketId, Long userId, Long tripId, int seats,
                   String ticketCode, String fromCity, String toCity) {}
```

- `ticket-service`: populate all fields from the `Ticket` entity
- `trip-service`: update record to include new fields (only uses `type`, `tripId`, `seats`)

### 2. notification-service (new, port 8084)

**Dependencies:** `spring-boot-starter-websocket`, `spring-kafka`, `jjwt-*`, `spring-boot-starter-actuator`, `lombok`  
No JPA, no Flyway, no PostgreSQL.

**Key classes:**
- `WebSocketConfig` — `@EnableWebSocketMessageBroker`, registers `/ws` endpoint, simple broker on `/user`, user destination prefix `/user`
- `WebSocketHandshakeInterceptor` — extracts JWT from `?token=` query param, writes `userId` into session attributes
- `UserHandshakeHandler extends DefaultHandshakeHandler` — creates `Principal` from `userId` attribute so STOMP user routing works
- `JwtService` — copy from other services (`extractUserId`, `isValid`)
- `SecurityConfig` — permit all (WS auth handled in handshake)
- `TicketEventConsumer` — `@KafkaListener` on `ticket.purchased` + `ticket.cancelled`, calls `SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/notifications", message)`
- `NotificationMessage` — record `(String type, String message, String ticketCode)`

### 3. nginx-microservices.conf

Add WebSocket proxy block:
```nginx
location /ws {
    proxy_pass http://notification-service:8084;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
}
```

### 4. docker-compose.yml (microservices)

Add `notification-service` container (port 8084), depends on `kafka`.

### 5. Angular frontend

**New:**
- `@stomp/stompjs` npm package (native WS, no SockJS needed)
- `NotificationService` — manages STOMP client, exposes `toasts` signal (array of `{id, type, message, ticketCode}`)
- `NotificationToastComponent` — overlaid fixed-position toast stack, auto-dismisses after 5s

**Modified:**
- `AuthService.login()` — call `NotificationService.connect(token)` after successful login
- `AuthService.logout()` — call `NotificationService.disconnect()`
- `AuthService.loadCurrentUser()` — reconnect WS if token exists on page load
- `app.ts` — include `<app-notification-toast>` in template

---

## Data flow (purchase)

1. Angular → `POST /api/tickets` → ticket-service
2. ticket-service saves ticket, publishes `TicketEvent{type=ticket.purchased, userId=42, ticketCode="ABC...", fromCity="Warszawa", toCity="Kraków", ...}` to Kafka
3. notification-service consumer receives event
4. `SimpMessagingTemplate.convertAndSendToUser("42", "/queue/notifications", NotificationMessage{type="purchased", message="Квиток Warszawa → Kraków куплено!", ticketCode="ABC..."})`
5. Angular STOMP subscription on `/user/queue/notifications` receives message → toast appears

---

## Dockerfile

Identical pattern to ticket-service. Multi-stage Maven build, eclipse-temurin:21-jre-alpine runtime, port 8084.

---

## Not in scope

- Persistence of notification history
- Email/SMS fallback
- Redis pub/sub for horizontal scaling
- K8s manifests (existing pattern is sufficient to replicate)
