CREATE TABLE filmoteka.outbox_event
(
    id           UUID PRIMARY KEY,
    topic        VARCHAR(255) NOT NULL,
    message_key  VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    created_ts   TIMESTAMPTZ  NOT NULL,
    published_ts TIMESTAMPTZ
);

CREATE INDEX idx_outbox_event_pending
    ON filmoteka.outbox_event (created_ts, id)
    WHERE published_ts IS NULL;