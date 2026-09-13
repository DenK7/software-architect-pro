# Диаграмма компонентов — Video Service

```mermaid
%% C4 Level 3 — Components. Video Service
flowchart TB
    gwext["API Gateway<br/>[Container]"]
    hubext["Домашний хаб<br/>[Container]"]
    mobileext["Мобильное приложение<br/>[Container]"]

    subgraph vs["Video Service [Container: Java / Spring Boot]"]
        archapi["Archive API<br/>[Component: REST Controller]<br/>Список записей, карточка, удаление"]
        uploadapi["Upload API<br/>[Component: REST Controller]<br/>Запрос ссылки на загрузку, подтверждение"]
        playapi["Playback API<br/>[Component: REST Controller]<br/>Выдача ссылки на просмотр"]
        urlsigner["Link Signer<br/>[Component]<br/>Подписанные ссылки с коротким сроком жизни"]
        quota["Quota Guard<br/>[Component]<br/>Объём архива и лимиты тарифа"]
        access["Access Checker<br/>[Component]<br/>Камера принадлежит дому пользователя"]
        meta["Recording Manager<br/>[Component]<br/>Метаданные записи и её статус"]
        retention["Retention Manager<br/>[Component]<br/>Срок хранения и удаление просроченного"]
        repo["Recording Repository<br/>[Component]<br/>Метаданные записей"]
        evtpub["Event Publisher<br/>[Component: Outbox]<br/>Событие о сохранённой записи"]
    end

    pg[("PostgreSQL<br/>[Database]")]
    minio[("MinIO<br/>[Object Storage]")]
    mq[("RabbitMQ<br/>[Message Broker]")]

    hubext -->|"REST, запрос ссылки и подтверждение"| uploadapi
    gwext -->|"REST, архив"| archapi
    gwext -->|"REST, просмотр"| playapi
    hubext -->|"HTTPS PUT по подписанной ссылке"| minio
    mobileext -->|"HTTPS GET по подписанной ссылке"| minio

    uploadapi --> access
    uploadapi --> quota
    uploadapi --> urlsigner
    uploadapi --> meta
    playapi --> access
    playapi --> urlsigner
    archapi --> access
    archapi --> repo
    meta --> repo
    meta --> evtpub
    retention --> repo
    retention -.-> minio
    urlsigner -.-> minio
    repo -.-> pg
    evtpub -.-> pg
    evtpub ==>|"запись сохранена"| mq

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class archapi,uploadapi,playapi,urlsigner,quota,access,meta,retention,repo,evtpub comp
    class gwext,hubext,mobileext ext
    class pg,minio,mq store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| Archive API | Список записей по камере и периоду, карточка записи, удаление |
| Upload API | Выдача ссылки на загрузку и приём подтверждения о завершении |
| Playback API | Выдача ссылки на просмотр конкретной записи |
| Link Signer | Формирование подписанных ссылок к MinIO с коротким сроком жизни |
| Quota Guard | Контроль объёма архива по тарифу до выдачи ссылки на запись |
| Access Checker | Проверка того, что камера относится к дому пользователя |
| Recording Manager | Метаданные записи и переход её статуса из «ожидается» в «сохранено» |
| Retention Manager | Удаление записей и файлов по истечении срока хранения |
| Recording Repository | Доступ к базе метаданных |
| Event Publisher | Публикация факта сохранения записи |

Сервис не пропускает через себя байты видео: он только решает, можно ли писать и читать, и выдаёт подписанную ссылку. Файл идёт от хаба в MinIO и из MinIO в приложение напрямую.

Живой поток в объём не входит — распознавание движения выполняется на камере или хабе, а событие идёт обычным путём устройства через Device Control. Video Service публикует только факт появления новой записи.
