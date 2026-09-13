# Диаграмма компонентов — User Service

```mermaid
%% C4 Level 3 — Components. User Service
flowchart TB
    gwext["API Gateway<br/>[Container]"]
    vaultext["Vault<br/>[External System]<br/>Секреты и параметры конфигурации"]

    subgraph usr["User Service [Container: Java / Spring Boot]"]
        userapi["User API<br/>[Component: REST Controller]<br/>Профиль, смена пароля, вход"]
        adminapi["Admin API<br/>[Component: REST Controller]<br/>Создание, изменение, удаление пользователей"]
        authsvc["Authentication Service<br/>[Component]<br/>Проверка учётных данных"]
        hasher["Password Hasher<br/>[Component]<br/>Хеширование и сверка паролей"]
        issuer["Token Issuer<br/>[Component]<br/>Выпуск JWT с асимметричной подписью"]
        signer["Token Signer<br/>[Component: клиент Vault]<br/>Подпись токена, закрытый ключ<br/>не покидает Vault"]
        keymeta["Key Metadata<br/>[Component]<br/>Идентификатор ключа, ротация,<br/>отдача открытого"]
        roles["Role Manager<br/>[Component]<br/>Роли пользователя и администратора"]
        repo["User Repository<br/>[Component]<br/>Учётные записи и профили"]
    end

    pg[("PostgreSQL<br/>[Database]")]

    gwext -->|"REST, операции пользователя"| userapi
    gwext -->|"REST, операции администратора"| adminapi
    gwext -->|"REST, запрос открытого ключа"| keymeta

    userapi --> authsvc
    authsvc --> hasher
    authsvc --> issuer
    issuer --> signer
    issuer --> keymeta
    signer -->|"подпись полезной нагрузки"| vaultext
    keymeta --> repo
    userapi --> repo
    adminapi --> roles
    adminapi --> repo
    roles --> repo
    repo -.-> pg

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class userapi,adminapi,authsvc,hasher,issuer,signer,keymeta,roles,repo comp
    class gwext,vaultext ext
    class pg store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| User API | Операции самого пользователя: профиль, смена пароля, вход |
| Admin API | Операции администратора: создание, редактирование и удаление пользователей |
| Authentication Service | Проверка учётных данных при входе |
| Password Hasher | Хеширование паролей и сверка при входе, пароли в открытом виде не хранятся |
| Token Issuer | Выпуск JWT с асимметричной подписью, срок жизни и состав полезной нагрузки |
| Token Signer | Подпись токена во внешнем Vault: полезная нагрузка уходит туда, обратно приходит подпись. Закрытый ключ не попадает ни в базу, ни в память сервиса |
| Key Metadata | Идентификатор ключа, срок действия, ротация и отдача открытого ключа шлюзу |
| Role Manager | Роли пользователя и администратора |
| User Repository | Доступ к собственной базе учётных записей |

Токены выпускает этот сервис, проверяет их шлюз. Общего секрета между сервисами нет — только открытый ключ, который можно раздавать свободно.

Закрытый ключ подписи в базе не хранится. Он живёт в Vault, и подпись выполняется там же: Token Signer отправляет полезную нагрузку и получает подпись обратно. В собственной базе остаются только метаданные ключа и открытая часть. Иначе закрытый ключ лежал бы в одной базе с таблицей учётных записей, и одна утечка давала бы возможность беззвучно выпускать токены от имени кого угодно.

Полномочия администратора ограничены учётными записями: создание, изменение, удаление, назначение ролей. Доступа к домам, устройствам и телеметрии пользователей у него нет — права нигде не пересекают границу арендатора, и отдельный механизм доступа поддержки не нужен.
