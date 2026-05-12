CREATE TABLE IF NOT EXISTS stations (
    id        BIGSERIAL    PRIMARY KEY,
    name      VARCHAR(255) UNIQUE NOT NULL,
    city      VARCHAR(255) NOT NULL,
    code      VARCHAR(255) UNIQUE NOT NULL,
    latitude  DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);
CREATE TABLE IF NOT EXISTS trains (
    id           BIGSERIAL    PRIMARY KEY,
    train_number VARCHAR(255) UNIQUE NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    total_seats  INTEGER      NOT NULL
);
CREATE TABLE IF NOT EXISTS routes (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS route_stops (
    id                 BIGSERIAL PRIMARY KEY,
    route_id           BIGINT    NOT NULL REFERENCES routes(id),
    station_id         BIGINT    NOT NULL REFERENCES stations(id),
    stop_order         INTEGER   NOT NULL,
    minutes_from_start INTEGER
);
CREATE TABLE IF NOT EXISTS trips (
    id              BIGSERIAL    PRIMARY KEY,
    train_id        BIGINT       NOT NULL REFERENCES trains(id),
    route_id        BIGINT       NOT NULL REFERENCES routes(id),
    departure_time  TIMESTAMP    NOT NULL,
    arrival_time    TIMESTAMP    NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED',
    available_seats INTEGER      NOT NULL
);
