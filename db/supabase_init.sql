-- =====================================================================
-- Zako — initial schema for Supabase Postgres
-- Paste into Supabase Dashboard → SQL Editor → Run
-- Idempotent: safe to re-run; uses IF NOT EXISTS / ON CONFLICT.
-- Mirrors the JPA entities in monolith/src/main/java/zako/monolith.
-- VARCHAR(255) used for String fields so Hibernate `ddl-auto=validate` passes.
-- =====================================================================

-- --- ENUMs (mirrored from Java enums; stored as text via Hibernate)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'role')           THEN CREATE TYPE role           AS ENUM ('USER', 'ADMIN'); END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'train_type')     THEN CREATE TYPE train_type     AS ENUM ('IC', 'TLK', 'EIC', 'EN', 'R', 'KM'); END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'trip_status')    THEN CREATE TYPE trip_status    AS ENUM ('SCHEDULED', 'DELAYED', 'CANCELLED', 'COMPLETED'); END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ticket_status')  THEN CREATE TYPE ticket_status  AS ENUM ('ACTIVE', 'USED', 'CANCELLED', 'EXPIRED'); END IF;
END $$;

-- =====================================================================
-- USERS
-- =====================================================================
CREATE TABLE IF NOT EXISTS users (
  id          BIGSERIAL PRIMARY KEY,
  first_name  VARCHAR(255)  NOT NULL,
  last_name   VARCHAR(255)  NOT NULL,
  email       VARCHAR(255)  NOT NULL UNIQUE,
  password    VARCHAR(255)  NOT NULL,
  role        role          NOT NULL DEFAULT 'USER',
  created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- =====================================================================
-- STATIONS
-- =====================================================================
CREATE TABLE IF NOT EXISTS stations (
  id         BIGSERIAL PRIMARY KEY,
  name       VARCHAR(255)     NOT NULL UNIQUE,
  city       VARCHAR(255)     NOT NULL,
  code       VARCHAR(255)     NOT NULL UNIQUE,
  latitude   DOUBLE PRECISION,
  longitude  DOUBLE PRECISION
);
CREATE INDEX IF NOT EXISTS idx_stations_name_lower ON stations (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_stations_city_lower ON stations (LOWER(city));

-- =====================================================================
-- TRAINS
-- =====================================================================
CREATE TABLE IF NOT EXISTS trains (
  id            BIGSERIAL PRIMARY KEY,
  train_number  VARCHAR(255)  NOT NULL UNIQUE,
  type          train_type    NOT NULL,
  total_seats   INTEGER       NOT NULL CHECK (total_seats > 0)
);

-- =====================================================================
-- ROUTES
-- =====================================================================
CREATE TABLE IF NOT EXISTS routes (
  id    BIGSERIAL PRIMARY KEY,
  name  VARCHAR(255) NOT NULL
);

-- =====================================================================
-- ROUTE_STOPS (ordered list of stations per route)
-- =====================================================================
CREATE TABLE IF NOT EXISTS route_stops (
  id                  BIGSERIAL PRIMARY KEY,
  route_id            BIGINT  NOT NULL REFERENCES routes (id)   ON DELETE CASCADE,
  station_id          BIGINT  NOT NULL REFERENCES stations (id) ON DELETE RESTRICT,
  stop_order          INTEGER NOT NULL CHECK (stop_order >= 0),
  minutes_from_start  INTEGER NOT NULL CHECK (minutes_from_start >= 0),
  CONSTRAINT uq_route_stop_order UNIQUE (route_id, stop_order)
);
CREATE INDEX IF NOT EXISTS idx_route_stops_route   ON route_stops (route_id);
CREATE INDEX IF NOT EXISTS idx_route_stops_station ON route_stops (station_id);

-- =====================================================================
-- TRIPS (a scheduled run of a train along a route)
-- =====================================================================
CREATE TABLE IF NOT EXISTS trips (
  id              BIGSERIAL PRIMARY KEY,
  train_id        BIGINT       NOT NULL REFERENCES trains (id)  ON DELETE RESTRICT,
  route_id        BIGINT       NOT NULL REFERENCES routes (id)  ON DELETE RESTRICT,
  departure_time  TIMESTAMP    NOT NULL,
  arrival_time    TIMESTAMP    NOT NULL,
  status          trip_status  NOT NULL DEFAULT 'SCHEDULED',
  available_seats INTEGER      NOT NULL CHECK (available_seats >= 0),
  CONSTRAINT chk_trip_times CHECK (arrival_time > departure_time)
);
CREATE INDEX IF NOT EXISTS idx_trips_route_date ON trips (route_id, departure_time);
CREATE INDEX IF NOT EXISTS idx_trips_train      ON trips (train_id);
CREATE INDEX IF NOT EXISTS idx_trips_departure  ON trips (departure_time);

-- =====================================================================
-- TICKETS
-- =====================================================================
CREATE TABLE IF NOT EXISTS tickets (
  id               BIGSERIAL PRIMARY KEY,
  user_id          BIGINT          NOT NULL REFERENCES users (id)    ON DELETE RESTRICT,
  trip_id          BIGINT          NOT NULL REFERENCES trips (id)    ON DELETE RESTRICT,
  from_station_id  BIGINT          NOT NULL REFERENCES stations (id) ON DELETE RESTRICT,
  to_station_id    BIGINT          NOT NULL REFERENCES stations (id) ON DELETE RESTRICT,
  seat_number      INTEGER,
  price            NUMERIC(10, 2)  NOT NULL CHECK (price >= 0),
  status           ticket_status   NOT NULL DEFAULT 'ACTIVE',
  ticket_code      VARCHAR(255)    NOT NULL UNIQUE,
  purchased_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_ticket_stations_differ CHECK (from_station_id <> to_station_id)
);
CREATE INDEX IF NOT EXISTS idx_tickets_user ON tickets (user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_trip ON tickets (trip_id);

-- =====================================================================
-- SEED: 20 Polish stations (idempotent via ON CONFLICT)
-- Mirrors monolith StationSeeder.java
-- =====================================================================
INSERT INTO stations (name, city, code, latitude, longitude) VALUES
  ('Warszawa Centralna',  'Warszawa',    'WAW', 52.2287, 21.0034),
  ('Warszawa Wschodnia',  'Warszawa',    'WWS', 52.2517, 21.0488),
  ('Kraków Główny',       'Kraków',      'KRK', 50.0682, 19.9447),
  ('Gdańsk Główny',       'Gdańsk',      'GDN', 54.3556, 18.6438),
  ('Gdynia Główna',       'Gdynia',      'GDY', 54.5226, 18.5306),
  ('Poznań Główny',       'Poznań',      'POZ', 52.4022, 16.9119),
  ('Wrocław Główny',      'Wrocław',     'WRO', 51.0989, 17.0367),
  ('Łódź Fabryczna',      'Łódź',        'LDZ', 51.7689, 19.4633),
  ('Łódź Kaliska',        'Łódź',        'LDK', 51.7592, 19.4214),
  ('Katowice',            'Katowice',    'KTW', 50.2581, 19.0167),
  ('Lublin',              'Lublin',      'LUB', 51.2342, 22.5688),
  ('Szczecin Główny',     'Szczecin',    'SZC', 53.4169, 14.5550),
  ('Białystok',           'Białystok',   'BIA', 53.1359, 23.1664),
  ('Bydgoszcz Główna',    'Bydgoszcz',   'BDG', 53.1346, 17.9919),
  ('Olsztyn Główny',      'Olsztyn',     'OLS', 53.7806, 20.4872),
  ('Rzeszów Główny',      'Rzeszów',     'RZE', 50.0419, 22.0023),
  ('Toruń Główny',        'Toruń',       'TRN', 53.0136, 18.6079),
  ('Częstochowa',         'Częstochowa', 'CZA', 50.7989, 19.0814),
  ('Zakopane',            'Zakopane',    'ZAK', 49.2992, 19.9496),
  ('Sopot',               'Sopot',       'SOP', 54.4416, 18.5604)
ON CONFLICT (code) DO NOTHING;

-- =====================================================================
-- Optional: a sample admin user. Password is BCrypt of "admin123".
-- Comment out if you want to create users only via the API.
-- =====================================================================
-- INSERT INTO users (first_name, last_name, email, password, role) VALUES
--   ('Admin', 'Root', 'admin@zako.local',
--    '$2a$10$A6r0HnL3W5sBQ9k1vR3p3O0hVZB8vZpL1xR2YqkCx5sKc.0GqYxO2', 'ADMIN')
-- ON CONFLICT (email) DO NOTHING;
