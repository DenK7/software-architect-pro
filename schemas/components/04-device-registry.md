# Диаграмма компонентов — Device Registry

```mermaid
%% C4 Level 3 — Components. Device Registry
flowchart TB
    gwext["API Gateway<br/>[Container]"]
    callers["Device Control, Automation,<br/>Catalog Service<br/>[Containers]"]
    homeext["Home Service<br/>[Container]"]

    subgraph dr["Device Registry [Container: Java / Spring Boot]"]
        devapi["Device API<br/>[Component: REST Controller]<br/>Список, карточка, регистрация, удаление"]
        typeapi["Device Type API<br/>[Component: REST Controller]<br/>Доступные типы и их возможности"]
        devmgr["Device Manager<br/>[Component]<br/>Жизненный цикл: регистрация, замена, вывод"]
        capres["Capability Resolver<br/>[Component]<br/>Возможности устройства по его типу"]
        refproj["Reference Projection<br/>[Component]<br/>Локальная копия справочника типов"]
        homeclient["Home Client<br/>[Component: REST]<br/>Подтверждение дома"]
        statetrack["Availability Tracker<br/>[Component]<br/>Последняя активность, перевод в офлайн"]
        repo["Device Repository<br/>[Component]<br/>Устройства и их паспорта"]
        evtpub["Event Publisher<br/>[Component: Outbox]<br/>События о регистрации, изменении, выводе"]
        evtsub["Reference Consumer<br/>[Component]<br/>Приём изменений справочника"]
    end

    pg[("PostgreSQL<br/>[Database]")]
    mq[("RabbitMQ<br/>[Message Broker]")]

    gwext -->|"REST, устройства пользователя"| devapi
    gwext -->|"REST, типы устройств"| typeapi
    callers -->|"REST, проверка устройства"| devapi
    homeclient -->|"REST"| homeext

    devapi --> devmgr
    typeapi --> refproj
    devmgr --> capres
    devmgr --> homeclient
    capres --> refproj
    devmgr --> repo
    devapi --> repo
    statetrack --> repo
    devmgr --> evtpub
    statetrack --> evtpub
    evtsub --> refproj

    repo -.-> pg
    evtpub -.-> pg
    refproj -.-> pg
    evtpub ==>|"события об устройствах"| mq
    mq ==>|"изменения справочников"| evtsub

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class devapi,typeapi,devmgr,capres,refproj,homeclient,statetrack,repo,evtpub,evtsub comp
    class gwext,callers,homeext ext
    class pg,mq store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| Device API | Устройства пользователя: список, карточка, регистрация, удаление |
| Device Type API | Выдача доступных типов устройств и их возможностей при подключении нового устройства |
| Device Manager | Жизненный цикл устройства: регистрация, замена, вывод из эксплуатации |
| Capability Resolver | Определение набора возможностей конкретного устройства по его типу |
| Reference Projection | Локальная копия справочника типов и возможностей — синхронных походов в Reference Data нет |
| Home Client | Подтверждение существования дома при регистрации устройства |
| Availability Tracker | Отметка последней активности и перевод устройства в офлайн при длительном молчании |
| Device Repository | Доступ к собственной базе устройств |
| Event Publisher | Публикация событий об устройствах через outbox, чтобы событие не терялось между коммитом и отправкой |
| Reference Consumer | Приём изменений справочника и обновление локальной проекции |

Этот сервис вырастает из CRUD монолита: `sensors` разрезается по вертикали, и сюда уходят паспорт устройства и его размещение, а значения измерений — в Telemetry.
