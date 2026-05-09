CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS stations (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255) UNIQUE NOT NULL,
    city      VARCHAR(255) NOT NULL,
    code      VARCHAR(255) UNIQUE NOT NULL,
    latitude  DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS trains (
    id           BIGSERIAL PRIMARY KEY,
    train_number VARCHAR(255) UNIQUE NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    total_seats  INTEGER      NOT NULL
);

CREATE TABLE IF NOT EXISTS routes (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS route_stops (
    id         BIGSERIAL PRIMARY KEY,
    route_id   BIGINT  NOT NULL REFERENCES routes(id),
    station_id BIGINT  NOT NULL REFERENCES stations(id),
    stop_order INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS trips (
    id               BIGSERIAL PRIMARY KEY,
    train_id         BIGINT      NOT NULL REFERENCES trains(id),
    route_id         BIGINT      NOT NULL REFERENCES routes(id),
    departure_time   TIMESTAMP   NOT NULL,
    arrival_time     TIMESTAMP   NOT NULL,
    status           VARCHAR(50) NOT NULL,
    available_seats  INTEGER     NOT NULL
);

CREATE TABLE IF NOT EXISTS tickets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT         NOT NULL REFERENCES users(id),
    trip_id         BIGINT         NOT NULL REFERENCES trips(id),
    from_station_id BIGINT         NOT NULL REFERENCES stations(id),
    to_station_id   BIGINT         NOT NULL REFERENCES stations(id),
    seat_number     INTEGER,
    price           NUMERIC(10, 2) NOT NULL,
    status          VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    ticket_code     VARCHAR(255)   UNIQUE NOT NULL,
    purchased_at    TIMESTAMP      NOT NULL DEFAULT now()
);
