# Диаграмма компонентов — Reference Data

```mermaid
%% C4 Level 3 — Components. Reference Data
flowchart TB
    gwext["API Gateway<br/>[Container]"]
    consumers["Device Registry<br/>[Container]"]

    subgraph rd["Reference Data [Container: Java / Spring Boot]"]
        readapi["Reference API<br/>[Component: REST Controller]<br/>Чтение справочников"]
        adminapi["Reference Admin API<br/>[Component: REST Controller]<br/>Ведение справочников администратором"]
        typemgr["Device Type Manager<br/>[Component]<br/>Типы устройств и их возможности"]
        metricmgr["Metric Manager<br/>[Component]<br/>Метрики, единицы измерения, допустимые диапазоны"]
        protomgr["Protocol Manager<br/>[Component]<br/>Поддерживаемые протоколы и производители"]
        classmgr["Classifier Manager<br/>[Component]<br/>Классификаторы статусов и перечисления"]
        version["Version Manager<br/>[Component]<br/>Версии справочника и совместимость"]
        repo["Reference Repository<br/>[Component]<br/>Справочные таблицы"]
        evtpub["Event Publisher<br/>[Component: Outbox]<br/>Публикация изменений справочников"]
    end

    pg[("PostgreSQL<br/>[Database]")]
    mq[("RabbitMQ<br/>[Message Broker]")]

    gwext -->|"REST, чтение справочников"| readapi
    gwext -->|"REST, ведение справочников"| adminapi

    readapi --> typemgr
    readapi --> metricmgr
    readapi --> protomgr
    readapi --> classmgr
    adminapi --> typemgr
    adminapi --> metricmgr
    adminapi --> protomgr
    adminapi --> classmgr
    adminapi --> version
    typemgr --> repo
    metricmgr --> repo
    protomgr --> repo
    classmgr --> repo
    version --> repo
    version --> evtpub
    repo -.-> pg
    evtpub -.-> pg
    evtpub ==>|"изменения справочников"| mq
    mq ==>|"обновление локальной проекции"| consumers

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class readapi,adminapi,typemgr,metricmgr,protomgr,classmgr,version,repo,evtpub comp
    class gwext,consumers ext
    class pg,mq store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| Reference API | Чтение справочников клиентскими приложениями |
| Reference Admin API | Ведение справочников администратором: добавление типов, метрик, протоколов |
| Device Type Manager | Типы устройств и наборы их возможностей |
| Metric Manager | Метрики, единицы измерения, допустимые диапазоны значений |
| Protocol Manager | Поддерживаемые протоколы подключения и производители устройств |
| Classifier Manager | Классификаторы статусов и прочие перечисления |
| Version Manager | Версия справочника и совместимость с потребителями |
| Reference Repository | Доступ к справочным таблицам |
| Event Publisher | Публикация изменений, по которым потребители обновляют локальные копии |

Этот сервис — механизм расширяемости платформы. Подключение нового типа устройства партнёра сводится к записи в справочнике: не нужен ни релиз реестра, ни изменение схемы данных. Именно так закрывается требование ТЗ про «будущее неуточнённое поведение».

Синхронно к нему на каждый запрос никто не ходит: потребители держат локальные проекции и обновляют их по событиям. Иначе справочник станет общей базой для всех сервисов и превратит систему в распределённый монолит.
