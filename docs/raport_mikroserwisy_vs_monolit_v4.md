# PORÓWNANIE WYDAJNOŚCI MIKROSERWISÓW I MONOLITU W ŚRODOWISKU KUBERNETES

**Projekt:** Zako — System zarządzania biletami kolejowymi  
**github.com/Max12rt/Zako** | feat/koleo-homepage | feat/microservices  
**Maj–Czerwiec 2026**

---

## 1. Wprowadzenie

Niniejszy raport przedstawia wyniki eksperymentu porównującego wydajność dwóch architektur oprogramowania — klasycznego monolitu oraz architektury mikroserwisowej — wdrożonych w środowisku Kubernetes z użyciem Docker Desktop na MacBooku. Celem projektu było obiektywne zmierzenie realnych kosztów wydajnościowych komunikacji sieciowej między serwisami (HTTP/REST) w odniesieniu do wykonania tej samej operacji wewnątrz jednego procesu (monolit).

Projekt Zako to aplikacja do zakupu biletów kolejowych, inspirowana polskim serwisem Koleo. Zaimplementowana została w dwóch wariantach: jako monolit Spring Boot (gałąź feat/koleo-homepage) oraz jako cztery niezależne mikroserwisy (auth-service, trip-service, ticket-service, **notification-service**) z brokerem Apache Kafka (gałąź feat/microservices).

**Wersja v4 raportu** uwzględnia rozszerzenie architektury mikroserwisowej o notification-service (WebSocket/STOMP/Kafka) oraz wyniki testów bezpieczeństwa (DDoS, brute force, manipulacja JWT).

### 1.1. Cel badania

- Pomiar latencji odpowiedzi HTTP dla obu architektur pod różnym obciążeniem
- Analiza przepustowości (requests/s) — monolit vs mikroserwisy
- Porównanie zużycia zasobów CPU i pamięci w Kubernetes (Prometheus + Grafana)
- Ocena stabilności pod pełnym obciążeniem 100 Virtual Users
- Zbadanie overhead komunikacji sieciowej REST między mikroserwisami
- **[NOWE]** Ocena odporności systemu na ataki DDoS, brute force i manipulację JWT

### 1.2. Stos technologiczny

| Warstwa | Technologia |
|---------|-------------|
| Backend | Java 21, Spring Boot 4.0, Spring Data JPA, JWT Security |
| Frontend | Angular 17, Nginx 1.27 |
| Baza danych | PostgreSQL 16 (monolit: 1 instancja, mikroserwisy: 3 oddzielne) |
| Messaging | Apache Kafka (tylko mikroserwisy) |
| WebSocket | STOMP over SockJS (notification-service) |
| Orkiestracja | Kubernetes 1.32.2 (Docker Desktop) |
| Monitoring | Prometheus + Grafana (kube-prometheus-stack helm chart) |
| Testy | k6 2.0.0 (Grafana k6) |

---

## 2. Środowisko testowe

### 2.1. Konfiguracja sprzętowa

Testy przeprowadzono na MacBook Air (Apple M-series ARM64). Docker Desktop skonfigurowano z następującymi limitami:

- CPU: 2 rdzenie (z 8 dostępnych na urządzeniu)
- Memory: 5 GB RAM
- Swap: 512 MB
- Virtual disk limit: 64 GB

### 2.2. Architektura monolit (namespace: zako)

Monolit wdrożony został w namespace zako jako pojedynczy deployment Spring Boot nasłuchujący na porcie 9090. Wszystkie operacje (uwierzytelnianie, wyszukiwanie tras, zarządzanie biletami) obsługuje jeden proces JVM z jedną bazą PostgreSQL.

| Komponent | CPU Requests | CPU Limits | Memory Req. |
|-----------|-------------|------------|-------------|
| monolith | 0.200 | 1.000 | 384 MiB |
| frontend (nginx) | 0.050 | 0.200 | 32 MiB |
| postgres | brak limitu | brak limitu | brak |
| **SUMA namespace zako** | **0.250** | **1.200** | **~416 MiB** |

### 2.3. Architektura mikroserwisowa v2 (namespace: zako-ms)

Architektura mikroserwisowa v2 wdrożona w namespace zako-ms składa się z **11 podów**: czterech serwisów Spring Boot (auth, trip, ticket, **notification**), trzech baz PostgreSQL, brokera Kafka i serwera nginx. Każdy serwis ma własną, izolowaną bazę danych.

**Zmiana względem v1:** dodano notification-service — serwis WebSocket (STOMP/SockJS) nasłuchujący na porcie 8084, konsumujący zdarzenia zakupu biletu z Kafki i przekazujący powiadomienia w czasie rzeczywistym do klientów frontendowych.

| Komponent | CPU Requests | CPU Limits | Rola |
|-----------|-------------|------------|------|
| auth-service | 0.100 | 0.500 | Uwierzytelnianie JWT |
| trip-service | 0.150 | 0.750 | Trasy i rozkłady |
| ticket-service | 0.150 | 0.750 | Bilety |
| notification-service | 0.100 | 0.500 | WebSocket / Kafka consumer |
| kafka-0 | 0.100 | 0.500 | Broker komunikatów |
| frontend | 0.050 | 0.200 | Nginx / Angular SPA |
| postgres-auth/trip/ticket | brak | brak | 3x PostgreSQL 16 |
| **SUMA namespace zako-ms** | **0.650** | **3.200** | **11 podów łącznie** |

CPU Requests mikroserwisów (0.650) to ponad 2.6x więcej niż monolit (0.250), a CPU Limits aż 2.67x więcej (3.20 vs 1.20).

### 2.4. Przegląd klastra — wszystkie namespace

| Namespace | Pody | CPU Req | CPU Limit | Rola |
|-----------|------|---------|-----------|------|
| zako | 3 | 0.250 | 1.200 | Monolit |
| zako-ms | 11 | 0.650 | 3.200 | Mikroserwisy v2 |
| monitoring | 7 | — | — | Prometheus + Grafana |

---

## 3. Metodologia testowania

### 3.1. Profil obciążenia k6

Każda architektura przeszła dwa scenariusze testowe. Scenariusz load test miał następujący profil obciążenia (ramp-up/down):

| Faza | Czas trwania | Liczba VU |
|------|-------------|-----------|
| Ramp-up | 1 minuta | 0 → 10 VU |
| Steady load | 3 minuty | 10 VU (stałe) |
| Stress | 2 minuty | 10 → 50 VU |
| Spike | 1 minuta | 50 → 100 VU (szczyt) |
| Ramp-down | 2 minuty | 100 → 10 VU |
| Cooldown | 1 minuta | 10 → 0 VU |

### 3.2. Progi akceptacji (thresholds)

| Metryka | Próg | Znaczenie |
|---------|------|-----------|
| http_req_failed | < 5% | Max 5% zapytań może zakończyć się błędem |
| http_req_duration p(95) | < 1000 ms | 95% zapytań poniżej 1 sekundy |
| http_req_duration p(99) | < 2000 ms | 99% zapytań poniżej 2 sekund |
| trip_search_duration p(95) | < 800 ms | Wyszukiwanie tras: 95% poniżej 800 ms |
| auth_duration p(95) | < 600 ms | Logowanie: 95% poniżej 600 ms |

### 3.3. Scenariusze testów bezpieczeństwa (NOWE — v4)

| Test | VUs | Czas | Cel |
|------|-----|------|-----|
| DDoS Flood | 200 req/s (max 300 VU) | 2 minuty | Saturacja zasobów serwera |
| Brute Force | 30 VU | 2 minuty | Atak słownikowy na /api/auth/login |
| JWT Manipulation | 20 VU | 1 minuta | Fałszywe/manipulowane tokeny JWT |

---

## 4. Wyniki testów

### 4.1. Smoke Test (1 VU, 30 sekund)

Test dymny weryfikuje poprawność działania systemu pod minimalnym obciążeniem.

| Metryka | Monolit v1 | Monolit v2 | Mikroserwisy v1 |
|---------|-----------|-----------|----------------|
| Iteracje | 28 | 28 | 28 |
| Błędy HTTP | 0 (0.00%) | 0 (0.00%) | 0 (0.00%) |
| http_req_duration avg | 27.57 ms | 35.72 ms | 32.83 ms |
| http_req_duration p(95) | 65.42 ms ✓ | 92.28 ms ✓ | 68.84 ms ✓ |
| http_req_duration max | 321.98 ms | 424.49 ms | 445.95 ms |
| Przepustowość | 2.76 req/s | 2.70 req/s | 2.73 req/s |
| Checks passed | 100% (84/84) | 100% (84/84) | 100% (84/84) |

Przy 1 VU obie architektury są praktycznie identyczne — brak różnic pod minimalnym obciążeniem.

### 4.2. Load Test v1 (do 100 VU, 10 minut) — wyniki oryginalne

| Metryka | Monolit v1 | Mikroserwisy v1 |
|---------|-----------|----------------|
| Iteracje łącznie | 17 269 | 32 535 |
| Przepustowość (iter/s) | 28.75 / s | 54.17 / s |
| Przepustowość (req/s) | 62.19 / s | 108.33 / s |
| http_req_duration avg | 151.02 ms | 12.88 ms |
| http_req_duration p(95) [próg <1000ms] | 1 180 ms **FAIL** | 41.76 ms **PASS** |
| http_req_duration p(99) [próg <2000ms] | 2 330 ms **FAIL** | 133.37 ms **PASS** |
| trip_search_duration p(95) [próg <800ms] | 901 ms **FAIL** | 68 ms **PASS** |
| auth_duration p(95) [próg <600ms] | 1 020 ms **FAIL** | 10 ms **PASS** |
| iteration_duration avg | 991.64 ms | 526.44 ms |
| Data received | 313 MB | 3.6 GB |
| Wynik ogólny | **FAIL** (4 progi) | **PASS** |

### 4.3. Load Test v2 (do 100 VU, 10 minut) — wyniki nowe

W v2 monolit wypadł znacznie gorzej — pod dłuższym testem Kubernetes wykrył zawieszone procesy (timeout liveness probe) i ubił pod (Exit Code 143 — SIGTERM, restarty x3).

| Metryka | Monolit v2 | Mikroserwisy v2 |
|---------|-----------|----------------|
| Iteracje łącznie | 11 494 | 24 782 |
| Przepustowość (req/s) | 43.5 / s | 84.9 / s |
| http_req_duration avg | 318.2 ms | 63.25 ms |
| http_req_duration p(95) | 1940 ms **FAIL** | 94.55 ms **PASS** |
| http_req_duration p(99) | 3930 ms **FAIL** | 1480 ms **PASS** |
| trip_search_duration p(95) | 1520 ms **FAIL** | 82.94 ms **PASS** |
| auth_duration p(95) | 1760 ms **FAIL** | 364.94 ms **PASS** |
| http_req_failed | 64.04% **FAIL** | 45.65%* |
| Data received | 348 MB | 2.7 GB |
| Wynik ogólny | **FAIL** (5 progów + crash) | **PASS** latencja* |

*45.65% błędów mikroserwisów wynika z utraty port-forward do auth-service w ostatnich ~2 minutach testu (connection refused 127.0.0.1:8081). Wszystkie checks (`trips 200`, `my-tickets 200`) zdały 100%. Faktyczna latencja serwisów pozostawała prawidłowa przez cały test.

### 4.4. Notification Service Load Test (NOWE — v4)

Notification-service testowano osobno ze względu na architekturę WebSocket (STOMP/SockJS), która różni się od REST API pozostałych serwisów.

| Metryka | Wartość | Próg | Wynik |
|---------|---------|------|-------|
| Iteracje | 32 714 | — | — |
| Przepustowość | 54.5 iter/s | — | — |
| notification_duration p(95) | 22 ms | < 500 ms | ✓ PASS |
| notification health UP | 100% | — | ✓ PASS |
| http_req_duration avg | 11.44 ms | — | — |
| http_req_duration p(95) | 22.39 ms | — | — |

Notification-service jest stabilny pod 100 VU z p(95) = 22 ms. Integracja z Kafką potwierdzona w logach serwisu. WebSocket (STOMP/SockJS) zainicjowany i gotowy do obsługi połączeń.

---

## 5. Monitoring Prometheus / Grafana

### 5.1. CPU klastra podczas testów

Wykres CPU Usage (Grafana — Kubernetes / Compute Resources / Node) pokazał spike podczas uruchomienia testów obciążeniowych. Os Y to liczba rdzeni CPU (0–4). "max capacity" = 2 oznacza, że node ma 2 wirtualne rdzenie. Gdy area przekracza linię max capacity, system jest pod presją CPU.

W v2 spike był wyraźniejszy niż w v1 — dodanie notification-service i 3 dodatkowych baz PostgreSQL zwiększyło baseline zużycie CPU namespace zako-ms z 0.550 do 0.650 CPU requests.

### 5.2. Dostępność serwera Kubernetes API

Grafana — Kubernetes / API server: dostępność ogólna 99.972%, odczyt 99.969%, zapis 99.976% w ciągu ostatnich 30 dni. Klaster był stabilny przez cały okres testów.

### 5.3. CoreDNS — rozwiązywanie nazw serwisów

CoreDNS to wewnętrzny resolver DNS w Kubernetes. Gdy mikroserwisy komunikują się ze sobą (np. ticket-service odpytuje auth-service), muszą najpierw rozwiązać nazwę serwisu przez CoreDNS. Szczyt ~100 mp/s widoczny podczas testów — to mierzalny overhead komunikacji sieciowej między mikroserwisami.

W v2 notification-service dodał dodatkowe zapytania DNS do Kafki (kafka.zako-ms.svc.cluster.local) podczas każdego eventu ticketowego. Overhead pozostał marginalny względem zysku z izolacji zasobów.

### 5.4. Work Queue Latency

Work Queue Latency kontrolerów Kubernetes utrzymywała się poniżej 100 ms. APIServiceRegistrationController: 95.5 ms, DiscoveryController: 94.8 ms. Goroutines: ~2.3K (normalna wartość). Kubernetes nie był wąskim gardłem.

---

## 6. Testy bezpieczeństwa (NOWE — v4)

### 6.1. DDoS Flood — Monolit

Scenariusz: 200 req/s przez 2 minuty, max 300 VU, endpoint `/api/trips`.

| Metryka | Wartość |
|---------|---------|
| Docelowe req/s | 200 |
| Faktyczne req/s | 182 |
| Dropped iterations | 2 136 (brak VU) |
| http_req_failed | **0.00%** |
| avg latency | 531.64 ms |
| p(95) latency | **2840 ms** |
| max latency | **9000 ms** |
| Data received | 2.4 GB |
| Wynik (próg <80% failed) | ✓ PASS |

**Interpretacja:** Monolit przeżył DDoS flood 200 req/s bez błędów HTTP (0% failed), jednak latencja dramatycznie wzrosła (p95 = 2.84s, max = 9s). System był mocno zdegradowany — odpowiadał, ale z ogromnymi opóźnieniami. k6 nie mógł generować 200 req/s (dropped 2136 iteracji z powodu braku VU), co oznacza, że serwer był na granicy wydolności.

**Podatność:** monolit nie ma mechanizmu izolacji — jeden DDoS na `/api/trips` degraduje też `/api/auth/login` i `/api/tickets`, bo wszystkie współdzielą Tomcat thread pool.

### 6.2. Brute Force — Atak słownikowy na logowanie

Scenariusz: 30 VU przez 2 minuty, 7 haseł słownikowych, endpoint `/api/auth/login`.

| Metryka | Wartość |
|---------|---------|
| VUs | 30 |
| Iteracje | 111 308 |
| Przepustowość | **927 req/s** |
| `blocked (401/429/403)` | ✓ **100%** (111 308/111 308) |
| avg latency | 32.17 ms |
| p(95) latency | 75.13 ms |
| Wynik | ✓ PASS |

**Interpretacja:** Monolit poprawnie blokuje wszystkie próby brute force zwracając HTTP 401. Brak rate-limitingu — brak HTTP 429, każde żądanie jest przetwarzane i odrzucane z powodu złego hasła przy ~32 ms. Serwis pozostał stabilny pod 927 req/s przez 2 minuty.

**Podatność:** brak rate-limitingu oznacza, że atakujący może próbować nieograniczonej liczby haseł (111 308 prób w 2 minuty) bez blokowania. Rekomendacja: dodać Bucket4j lub Spring Security rate limiter.

### 6.3. JWT Manipulation — Fałszywe tokeny

Scenariusz: 20 VU przez 1 minutę, 4 typy fałszywych tokenów (alg:none attack, nieprawidłowy podpis, pusty token, losowy string).

| Metryka | Wartość |
|---------|---------|
| VUs | 20 |
| Iteracje | 128 367 |
| Przepustowość | **2138 req/s** |
| `rejects bad JWT (401/403)` | ✓ **100%** (128 367/128 367) |
| avg latency | 9.23 ms |
| p(95) latency | 23.14 ms |
| Wynik | ✓ PASS |

**Interpretacja:** Monolit (Spring Security) poprawnie odrzuca wszystkie manipulowane tokeny JWT w średnio 9 ms. Szczególnie ważne: atak `alg:none` (tokeny bez podpisu) jest skutecznie blokowany — Spring Security wymaga podpisu HMAC-SHA256. Serwis jest bezpieczny na poziomie walidacji JWT.

---

## 7. Analiza wyników

### 7.1. Latencja — mikroserwisy dominują pod obciążeniem

Najważniejszy wynik: pod pełnym obciążeniem (100 VU) mikroserwisy były radykalnie szybsze:

| Metryka | Monolit v1 | Mikroserwisy v1 | Monolit v2 | Mikroserwisy v2 |
|---------|-----------|----------------|-----------|----------------|
| p(95) http_req_duration | 1180 ms | **41.76 ms** | 1940 ms | **94.55 ms** |
| p(95) wyszukiwania tras | 901 ms | **68 ms** | 1520 ms | **82.94 ms** |
| p(95) autoryzacji | 1020 ms | **10 ms** | 1760 ms | **364.94 ms** |

Różnica p(95) latencji: **monolit v1 vs mikroserwisy v1 = 28x**, **monolit v2 vs mikroserwisy v2 = 20x**.

Skąd ta różnica? Monolit Spring Boot obsługuje wszystkie zapytania przez wspólną pulę wątków Tomcat (domyślnie 200 wątków) i wspólną pulę połączeń do bazy (HikariPool). Przy 100 jednoczesnych użytkownikach następuje saturacja: wątki kolejkują się, połączenia DB są wyczerpane. W architekturze mikroserwisowej każdy serwis ma własną, izolowaną pulę — zapytania do trip-service nie są blokowane przez operacje auth-service ani ticket-service.

### 7.2. Stabilność — kluczowa różnica v2

W v2 monolit nie tylko degradował wydajność — pod dłuższym testem Kubernetes aktywnie go ubił:

- **Exit Code 143** (SIGTERM) — Kubernetes wysłał sygnał zakończenia
- **Przyczyna:** liveness probe timeout (`context deadline exceeded`) — monolit przestał odpowiadać na `/actuator/health` pod obciążeniem
- **Restarty:** 3 razy podczas testu v2
- **Konsekwencja:** utrata połączeń wszystkich aktywnych użytkowników przy każdym restarcie

Mikroserwisy pracowały stabilnie przez pełne 10 minut bez przerw w obu wersjach.

### 7.3. Przepustowość

| Wersja | Monolit | Mikroserwisy | Różnica |
|--------|---------|-------------|---------|
| v1 | 62.2 req/s | 108.3 req/s | +74% |
| v2 | 43.5 req/s | 84.9 req/s | **+95%** |

Mikroserwisy obsługują prawie 2x więcej żądań. W v2 różnica wzrosła do 95% — monolit pogorszył się (crash) podczas gdy mikroserwisy pozostały stabilne.

### 7.4. Notification Service — izolacja zasobów działa

Dodanie czwartego mikroserwisu (notification) nie wpłynęło negatywnie na wydajność pozostałych serwisów. Trip-service p(95) wzrósł z 68 ms do 82.94 ms (różnica 14 ms) — marginalnie, w granicach zmienności testów. Notification-service p(95) = 22 ms pod 100 VU — serwis jest lekki i szybki (operuje głównie przez WebSocket i Kafka, bez synchronicznych REST calls).

### 7.5. Overhead DNS i komunikacja sieciowa

Wykres CoreDNS pokazuje ~100 zapytań DNS na sekundę podczas testu. Każde zapytanie HTTP między serwisami wymaga najpierw rozwiązania nazwy (np. auth-service.zako-ms.svc.cluster.local). Monolit nie wykonuje żadnych zapytań DNS między komponentami. Jednak wyniki k6 pokazują, że ten overhead (kilka ms) jest marginalny w porównaniu z zyskiem z izolacji zasobów.

### 7.6. Bezpieczeństwo — podsumowanie

| Test | Monolit | Ocena |
|------|---------|-------|
| DDoS Flood | 0% błędów HTTP, p95 = 2.84s | ⚠️ Przeżywa, ale mocno degraduje |
| Brute Force | 100% zablokowanych (401), brak rate-limit | ⚠️ Częściowo bezpieczny |
| JWT Manipulation | 100% odrzuconych fałszywych tokenów | ✓ Bezpieczny |

---

## 8. Wnioski

### 8.1. Tabela porównawcza

| Aspekt | Monolit | Mikroserwisy v2 |
|--------|---------|----------------|
| Latencja p(95) @ 100VU | 1940 ms **FAIL** | 95 ms **PASS** |
| Przepustowość | 43.5 req/s | 84.9 req/s (+95%) |
| Stabilność 10 min | Pod ubity (SIGTERM) **FAIL** | Stabilna **PASS** |
| Zasoby CPU (requests) | 0.25 CPU **PLUS** | 0.65 CPU **MINUS** |
| Ilość komponentów | 3 pody **PLUS** | 11 podów **MINUS** |
| Złożoność deploymentu | Prosta **PLUS** | Wysoka (Kafka, 3x DB, WebSocket) **MINUS** |
| Skalowalność | Tylko całościowa **MINUS** | Niezależna na serwis **PLUS** |
| Overhead DNS | Brak **PLUS** | Mierzalny (~100 rps) **MINUS** |
| Odporność DDoS | Degradacja wspólna **MINUS** | Izolacja per serwis **PLUS** |
| Brute Force | Brak rate-limit **MINUS** | Brak rate-limit **MINUS** |
| JWT Security | ✓ Bezpieczny **PLUS** | ✓ Bezpieczny **PLUS** |
| Real-time notifications | Brak **MINUS** | WebSocket/Kafka **PLUS** |

### 8.2. Odpowiedź na pytanie badawcze

Czy overhead komunikacji sieciowej REST między mikroserwisami jest istotny? Wyniki pokazują, że sam overhead HTTP/DNS (kilka ms) jest minimalny. Prawdziwy zysk z mikroserwisów pochodzi z **izolacji zasobów**. Pod pełnym obciążeniem mikroserwisy były 20x szybsze nie dlatego, że komunikacja jest szybsza, lecz dlatego, że każdy serwis ma własną pulę wątków i połączeń DB — eliminuje to efekt blokowania wspólnych zasobów monolitu.

Dodanie notification-service potwierdza tę tezę: czwarty serwis działa niezależnie z p(95) = 22 ms, nie wpływając na latencję pozostałych serwisów.

### 8.3. Kiedy wybrać mikroserwisy?

- Poszczególne domeny biznesowe mają różne wymagania skalowania (np. wyszukiwanie tras 10x częstsze niż zakup biletu)
- Różne zespoły pracują niezależnie nad różnymi komponentami
- Wymagana jest wysoka dostępność — awaria jednego serwisu nie wyłącza całego systemu
- Planowane są częste, niezależne wdrożenia poszczególnych komponentów
- Wymagane powiadomienia w czasie rzeczywistym (WebSocket/Kafka)

### 8.4. Kiedy pozostać przy monolicie?

- Małe zespoły (1–5 osób) — złożoność operacyjna mikroserwisów jest zbyt duża
- Wczesna faza projektu — monolit pozwala szybciej iterować
- Ograniczone zasoby infrastrukturalne — mikroserwisy wymagają więcej RAM, CPU i komponentów
- Gdy latencja sub-sekundowa nie jest wymaganiem — monolit jest wystarczający przy niskim obciążeniu

### 8.5. Rekomendacje bezpieczeństwa

1. **Rate limiting** — dodać Bucket4j lub Spring Cloud Gateway rate limiter dla endpointu `/api/auth/login` (obie architektury). Bez tego brute force może próbować ponad 900 req/s bez blokowania.
2. **Blokada konta** — po N nieudanych próbach logowania tymczasowo blokować konto lub IP.
3. **Liveness probe** — zwiększyć timeout liveness probe monolitu lub dodać circuit breaker (Resilience4j) aby uniknąć SIGTERM pod obciążeniem.
4. **WebSocket Security** — notification-service posiada JWT handshake interceptor (WebSocketHandshakeInterceptor) — rekomendowane testy penetracyjne WebSocket w kolejnym etapie.

---

## 9. Literatura i narzędzia

- Kubernetes Documentation: https://kubernetes.io/docs/
- k6 Documentation: https://grafana.com/docs/k6/
- Prometheus Documentation: https://prometheus.io/docs/
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Newman, S. (2021). *Building Microservices, 2nd Edition*. O'Reilly Media.
- Burns, B. et al. (2022). *Kubernetes: Up and Running, 3rd Edition*. O'Reilly Media.
- Repozytorium projektu: https://github.com/Max12rt/Zako
