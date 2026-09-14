# Диаграмма компонентов — Telemetry Service

```mermaid
%% C4 Level 3 — Components. Telemetry Service
flowchart TB
    gwext["API Gateway<br/>[Container]"]

    subgraph ts["Telemetry Service [Container: Java / Spring Boot]"]
        queryapi["Measurement API<br/>[Component: REST Controller]<br/>Текущее значение и история"]
        aggapi["Aggregate API<br/>[Component: REST Controller]<br/>Средние, минимумы, максимумы за период"]
        consumer["Measurement Consumer<br/>[Component]<br/>Приём измерений из брокера"]
        validator["Measurement Validator<br/>[Component]<br/>Известное устройство, известная метрика"]
        normalizer["Unit Normalizer<br/>[Component]<br/>Приведение единиц измерения к базовым"]
        writer["Ingest Writer<br/>[Component]<br/>Пакетная запись во временные ряды"]
        devproj["Device Projection<br/>[Component]<br/>Локальная копия реестра: устройство, дом, метрики"]
        devsub["Device Event Consumer<br/>[Component]<br/>Приём событий об устройствах"]
        retention["Retention Manager<br/>[Component]<br/>Прореживание и срок хранения рядов"]
        repo["Measurement Repository<br/>[Component]<br/>Доступ к временным рядам"]
    end

    tsdb[("PostgreSQL + TimescaleDB<br/>[Time-series Database]")]
    mq[("RabbitMQ<br/>[Message Broker]")]

    gwext -->|"REST, показания"| queryapi
    gwext -->|"REST, агрегаты"| aggapi

    mq ==>|"измерения"| consumer
    mq ==>|"события об устройствах"| devsub
    devsub --> devproj

    consumer --> validator
    validator --> devproj
    validator --> normalizer
    normalizer --> writer
    writer --> repo
    queryapi --> repo
    aggapi --> repo
    retention --> repo
    repo -.-> tsdb
    devproj -.-> tsdb

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class queryapi,aggapi,consumer,validator,normalizer,writer,devproj,devsub,retention,repo comp
    class gwext ext
    class tsdb,mq store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| Measurement API | Текущее значение и история измерений по устройству и метрике |
| Aggregate API | Агрегаты за период: средние, минимумы, максимумы |
| Measurement Consumer | Приём потока измерений из брокера |
| Measurement Validator | Отсев измерений от неизвестных устройств и по неизвестным метрикам |
| Unit Normalizer | Приведение единиц к базовым, чтобы агрегаты считались корректно |
| Ingest Writer | Пакетная запись во временные ряды — по одному измерению писать дорого |
| Device Projection | Локальная копия реестра: идентификатор, дом, набор метрик |
| Device Event Consumer | Обновление проекции по событиям реестра |
| Retention Manager | Прореживание старых рядов и удаление по сроку хранения |
| Measurement Repository | Доступ к гипертаблице измерений |

Синхронных вызовов в Device Registry на пути приёма измерений нет: проверка идёт по локальной проекции. Реестр может быть недоступен — поток данных от этого не прервётся.

Ключ измерения — пара «устройство и метрика», а не одно устройство: комбинированный датчик отдаёт температуру и влажность одновременно. Не-числовые значения (движение, открыто-закрыто) хранятся отдельными колонками, без JSONB.
