CREATE TABLE IF NOT EXISTS tickets (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    trip_id             BIGINT       NOT NULL,
    from_station_id     BIGINT       NOT NULL,
    from_station_name   VARCHAR(255),
    from_station_city   VARCHAR(255),
    from_station_code   VARCHAR(255),
    to_station_id       BIGINT       NOT NULL,
    to_station_name     VARCHAR(255),
    to_station_city     VARCHAR(255),
    to_station_code     VARCHAR(255),
    seat_number         INTEGER,
    price               NUMERIC(10,2) NOT NULL,
    status              VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE',
    payment_status      VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    ticket_code         VARCHAR(64)   UNIQUE NOT NULL,
    purchased_at        TIMESTAMP     NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tickets_user_id ON tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_trip_id ON tickets(trip_id);
