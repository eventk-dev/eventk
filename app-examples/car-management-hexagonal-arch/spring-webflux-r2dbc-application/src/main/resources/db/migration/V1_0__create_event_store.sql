CREATE TABLE event
(
    position    bigserial NOT NULL unique,
    stream_type text      NOT NULL,
    stream_id   uuid      NOT NULL,
    version     int       NOT NULL,
    payload     jsonb     NOT NULL,
    metadata    jsonb     NOT NULL,
    PRIMARY KEY (stream_id, version)
);
