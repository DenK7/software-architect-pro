import asyncio
import logging
import os
from contextlib import asynccontextmanager
from datetime import datetime

import httpx
import psycopg
from fastapi import FastAPI, HTTPException, Query
from psycopg.rows import dict_row

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@localhost:5434/telemetry")
MONOLITH_URL = os.getenv("MONOLITH_URL", "http://localhost:8080")
POLL_INTERVAL_SECONDS = float(os.getenv("POLL_INTERVAL_SECONDS", "10"))

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("telemetry")


def connect() -> psycopg.Connection:
    return psycopg.connect(DATABASE_URL, row_factory=dict_row)


def apply_schema() -> None:
    with open("schema.sql", encoding="utf-8") as handle:
        ddl = handle.read()
    with connect() as connection:
        connection.execute(ddl)
    log.info("Схема применена")


def store(sensors: list[dict]) -> int:
    rows = [
        (s["id"], s["last_updated"], s["value"], s.get("status"), s.get("unit"))
        for s in sensors
        if s.get("last_updated") is not None and s.get("value") is not None
    ]
    if not rows:
        return 0

    with connect() as connection, connection.cursor() as cursor:
        cursor.executemany(
            """
            INSERT INTO measurements (device_id, ts, value, status, unit)
            VALUES (%s, %s, %s, %s, %s)
            ON CONFLICT (device_id, ts) DO NOTHING
            """,
            rows,
        )
        return cursor.rowcount if cursor.rowcount and cursor.rowcount > 0 else 0


async def poll_forever() -> None:
    async with httpx.AsyncClient(base_url=MONOLITH_URL, timeout=5.0) as client:
        while True:
            try:
                response = await client.get("/api/v1/sensors")
                response.raise_for_status()
                sensors = response.json() or []
                written = await asyncio.to_thread(store, sensors)
                log.info("Опрос: получено %d, записано %d", len(sensors), written)
            except Exception as error:  # noqa: BLE001 — падать из-за монолита не надо
                log.warning("Монолит недоступен, ряд не пополнен: %s", error)
            await asyncio.sleep(POLL_INTERVAL_SECONDS)


@asynccontextmanager
async def lifespan(_: FastAPI):
    await asyncio.to_thread(apply_schema)
    task = asyncio.create_task(poll_forever())
    yield
    task.cancel()


api = FastAPI(title="Telemetry Service", version="1.0.0", lifespan=lifespan)


@api.get("/api/v1/measurements")
def measurements(
    device_id: int = Query(..., description="Идентификатор датчика из монолита"),
    since: datetime | None = Query(None, alias="from", description="Начало интервала"),
    until: datetime | None = Query(None, alias="to", description="Конец интервала"),
    limit: int = Query(100, ge=1, le=1000),
) -> list[dict]:
    sql = "SELECT device_id, ts, value, status, unit FROM measurements WHERE device_id = %s"
    params: list = [device_id]
    if since is not None:
        sql += " AND ts >= %s"
        params.append(since)
    if until is not None:
        sql += " AND ts <= %s"
        params.append(until)
    sql += " ORDER BY ts DESC LIMIT %s"
    params.append(limit)

    with connect() as connection, connection.cursor() as cursor:
        cursor.execute(sql, params)
        return cursor.fetchall()


@api.get("/api/v1/measurements/latest")
def latest(device_id: int = Query(..., description="Идентификатор датчика из монолита")) -> dict:
    with connect() as connection, connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT device_id, ts, value, status, unit
            FROM measurements
            WHERE device_id = %s
            ORDER BY ts DESC
            LIMIT 1
            """,
            (device_id,),
        )
        row = cursor.fetchone()

    if row is None:
        raise HTTPException(status_code=404, detail="Измерений по этому датчику пока нет")
    return row


@api.get("/health")
def health() -> dict:
    return {"status": "ok"}
