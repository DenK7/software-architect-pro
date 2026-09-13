# Диаграмма компонентов — Home Service

```mermaid
%% C4 Level 3 — Components. Home Service
flowchart TB
    gwext["API Gateway<br/>[Container]"]
    dregext["Device Registry<br/>[Container]"]
    autoext["Automation Service<br/>[Container]"]

    subgraph hs["Home Service [Container: Java / Spring Boot]"]
        homeapi["Home API<br/>[Component: REST Controller]<br/>Дома пользователя"]
        homemgr["Home Manager<br/>[Component]<br/>Создание и изменение дома"]
        settings["Home Settings<br/>[Component]<br/>Наименование, адрес, часовой пояс"]
        owner["Ownership Resolver<br/>[Component]<br/>Проверка принадлежности дома пользователю"]
        lookup["Home Lookup API<br/>[Component: REST Controller]<br/>Служебное чтение для других сервисов:<br/>существование дома, часовой пояс"]
        repo["Home Repository<br/>[Component]<br/>Дома пользователей"]
    end

    pg[("PostgreSQL<br/>[Database]")]

    gwext -->|"REST, дома"| homeapi
    dregext -->|"REST, проверка дома"| lookup
    autoext -->|"REST, часовой пояс"| lookup

    homeapi --> owner
    lookup --> owner
    homeapi --> homemgr
    homeapi --> settings
    homemgr --> repo
    settings --> repo
    owner --> repo
    lookup --> repo
    repo -.-> pg

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class homeapi,homemgr,settings,owner,lookup,repo comp
    class gwext,dregext,autoext ext
    class pg store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| Home API | Список домов пользователя, создание и изменение дома |
| Home Manager | Жизненный цикл дома: создание, переименование, удаление |
| Home Settings | Наименование, адрес и часовой пояс дома — часовой пояс нужен сценариям по расписанию |
| Ownership Resolver | Источник истины по вопросу «этот дом принадлежит этому пользователю» |
| Home Lookup API | Служебное чтение для соседних сервисов: Device Registry подтверждает существование дома при регистрации устройства, Automation забирает часовой пояс для расписаний. Отделено от пользовательского Home API, потому что вызывается сервисами, а не приложением |
| Home Repository | Доступ к собственной базе домов |

Дом считается единым пространством: деления на помещения нет, устройство привязывается прямо к дому. Это убирает из модели целый уровень — классификатор помещений, их создание и перенос устройств между ними.

Делегирования прав тоже нет: у дома один владелец, поэтому проверка доступа сводится к одному вопросу и живёт в единственном компоненте.
