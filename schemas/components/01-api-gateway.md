# Диаграмма компонентов — API Gateway

```mermaid
%% C4 Level 3 — Components. API Gateway
flowchart TB
    mobile["Мобильное приложение<br/>[Container]"]
    adminui["Админка<br/>[Container]"]
    services["Микросервисы платформы<br/>[Containers]"]
    usrext["User Service<br/>[Container]"]

    subgraph gw["API Gateway [Container: Spring Cloud Gateway]"]
        entry["Request Entry<br/>[Component]<br/>Терминация TLS, приём REST-запросов"]
        tokenv["Token Verifier<br/>[Component]<br/>Проверка подписи JWT открытым ключом"]
        jwks["Key Cache<br/>[Component]<br/>Кэш открытых ключей User Service"]
        ratelim["Rate Limiter<br/>[Component]<br/>Ограничение частоты по пользователю и адресу"]
        router["Route Resolver<br/>[Component]<br/>Сопоставление пути с целевым сервисом"]
        forwarder["Request Forwarder<br/>[Component]<br/>Проброс запроса без разбора тела"]
        errs["Error Normalizer<br/>[Component]<br/>Единый формат ошибок наружу"]
        audit["Audit Logger<br/>[Component]<br/>Журнал доступа: кто, куда, когда"]
    end

    redis[("Redis<br/>[Cache]")]

    mobile -->|"REST/JSON, HTTPS"| entry
    adminui -->|"REST/JSON, HTTPS"| entry
    entry --> tokenv
    tokenv --> jwks
    jwks -->|"REST, открытый ключ"| usrext
    tokenv --> ratelim
    ratelim --> router
    router --> forwarder
    forwarder -->|REST| services
    forwarder --> errs
    forwarder --> audit
    ratelim -.-> redis
    audit -.-> redis

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class entry,tokenv,jwks,ratelim,router,forwarder,errs,audit comp
    class mobile,adminui,services,usrext ext
    class redis store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| Request Entry | Терминация TLS, разбор заголовков, единая точка входа снаружи |
| Token Verifier | Проверка подписи и срока действия JWT, отклонение неавторизованных запросов |
| Key Cache | Хранение открытых ключей User Service, периодическое обновление |
| Rate Limiter | Счётчики частоты обращений по пользователю и адресу, отсечение злоупотреблений |
| Route Resolver | Определение целевого сервиса по пути запроса |
| Request Forwarder | Передача запроса дальше без разбора тела — шлюз не знает контрактов сервисов |
| Error Normalizer | Приведение ошибок сервисов к единому формату ответа |
| Audit Logger | Запись факта доступа для разбора инцидентов |

Бизнес-логики в шлюзе нет сознательно: он занимается только сквозными задачами. Собственной базы у него тоже нет — счётчики и журнал живут в Redis.
