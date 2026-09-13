CREATE TABLE IF NOT EXISTS measurements (
    device_id integer          NOT NULL,
    ts        timestamptz      NOT NULL,
    value     double precision NOT NULL,
    status    varchar(20),
    unit      varchar(20),
    PRIMARY KEY (device_id, ts)
);

CREATE INDEX IF NOT EXISTS idx_measurements_latest ON measurements (device_id, ts DESC);
