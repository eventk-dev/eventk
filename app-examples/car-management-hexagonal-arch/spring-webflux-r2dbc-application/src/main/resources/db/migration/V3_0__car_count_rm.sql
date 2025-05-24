CREATE TABLE car_count_rm__car
(
    id    uuid NOT NULL PRIMARY KEY,
    make  text NOT NULL,
    model text NOT NULL
);


CREATE TABLE car_count_rm__car_count
(
    make  text NOT NULL,
    model text NOT NULL,
    count int  not null,
    primary key (make, model)
);
