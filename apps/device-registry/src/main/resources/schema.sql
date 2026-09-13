CREATE TABLE IF NOT EXISTS devices (
    id            integer PRIMARY KEY,
    registered_at timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    deleted_at    timestamptz,
    status        varchar(20)  NOT NULL,
    unit          varchar(20),
    type          varchar(50)  NOT NULL,
    deleted_by    varchar(32),
    name          varchar(100) NOT NULL,
    location      varchar(100) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_devices_alive ON devices (id) WHERE deleted_at IS NULL;
