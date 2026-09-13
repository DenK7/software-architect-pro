# Диаграмма компонентов — Notification Service

```mermaid
%% C4 Level 3 — Components. Notification Service
flowchart TB
    gwext["API Gateway<br/>[Container]"]
    channelsext["Каналы доставки<br/>[External System]<br/>Push, электронная почта"]
    vaultext["Vault<br/>[External System]<br/>Ключ шифрования push-токенов"]

    subgraph ns["Notification Service [Container: Java / Spring Boot]"]
        histapi["History API<br/>[Component: REST Controller]<br/>Лента уведомлений пользователя"]
        prefapi["Preferences API<br/>[Component: REST Controller]<br/>Настройки: что и каким каналом присылать"]
        evtsub["Event Consumer<br/>[Component]<br/>Приём событий сценариев, устройств, записей"]
        rules["Notification Rules<br/>[Component]<br/>Нужно ли оповещать по этому событию"]
        prefs["Preference Resolver<br/>[Component]<br/>Каналы и тихие часы пользователя"]
        composer["Message Composer<br/>[Component]<br/>Шаблоны и формирование текста"]
        dedup["Deduplicator<br/>[Component]<br/>Подавление повторов и всплесков"]
        sender["Delivery Dispatcher<br/>[Component]<br/>Отправка в канал, повторы при сбое"]
        tokens["Device Token Store<br/>[Component]<br/>Push-токены приложений пользователя"]
        cipher["Token Cipher<br/>[Component: клиент Vault]<br/>Шифрование и расшифровка токенов"]
        repo["Notification Repository<br/>[Component]<br/>История отправленного и статусы"]
    end

    pg[("PostgreSQL<br/>[Database]")]
    mq[("RabbitMQ<br/>[Message Broker]")]

    gwext -->|"REST, лента"| histapi
    gwext -->|"REST, настройки"| prefapi
    mq ==>|"события сценариев, устройств, записей"| evtsub

    evtsub --> rules
    rules --> prefs
    prefs --> composer
    composer --> dedup
    dedup --> sender
    sender --> tokens
    tokens --> cipher
    cipher -->|"ключ шифрования"| vaultext
    sender -->|"HTTPS"| channelsext
    sender --> repo
    histapi --> repo
    prefapi --> prefs
    prefs --> repo
    tokens --> repo
    repo -.-> pg

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class histapi,prefapi,evtsub,rules,prefs,composer,dedup,sender,tokens,cipher,repo comp
    class gwext,channelsext,vaultext ext
    class pg,mq store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| History API | Лента уже отправленных уведомлений в приложении |
| Preferences API | Настройки пользователя: какие события и каким каналом присылать |
| Event Consumer | Приём событий о срабатывании сценариев, состоянии устройств и новых записях |
| Notification Rules | Решение о том, заслуживает ли событие уведомления |
| Preference Resolver | Каналы доставки и тихие часы конкретного пользователя |
| Message Composer | Формирование текста по шаблону с подстановкой устройства, дома и времени |
| Deduplicator | Подавление повторов: датчик, срабатывающий каждую минуту, не должен давать сто уведомлений |
| Delivery Dispatcher | Отправка во внешний канал и повтор при сбое доставки |
| Device Token Store | Push-токены приложений пользователя, хранятся в зашифрованном виде |
| Token Cipher | Шифрование и расшифровка push-токенов ключом из внешнего Vault. Токен предъявляемый, хешем его не заменить: при отправке нужно исходное значение |
| Notification Repository | История отправленного и статусы доставки |

Сервис полностью событийный: сам он никого не опрашивает, а реагирует на то, что публикуют другие. Наружу уходит через внешние каналы доставки — push-сервис платформы и электронную почту.
