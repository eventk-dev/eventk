CREATE TABLE car_write_helper
(
    id  uuid NOT NULL PRIMARY KEY,
    vin text NOT NULL UNIQUE
);
