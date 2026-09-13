# Диаграмма компонентов — Catalog Service

```mermaid
%% C4 Level 3 — Components. Catalog Service
flowchart TB
    gwext["API Gateway<br/>[Container]"]
    dregext["Device Registry<br/>[Container]"]

    subgraph cs["Catalog Service [Container: Java / Spring Boot]"]
        catapi["Catalog API<br/>[Component: REST Controller]<br/>Витрина комплектов"]
        orderapi["Order API<br/>[Component: REST Controller]<br/>Оформление и статус заказа"]
        actapi["Activation API<br/>[Component: REST Controller]<br/>Активация комплекта по серийным номерам"]
        kitmgr["Kit Manager<br/>[Component]<br/>Состав комплекта и его описание"]
        ordermgr["Order Manager<br/>[Component]<br/>Жизненный цикл заказа"]
        serial["Serial Validator<br/>[Component]<br/>Проверка серийного номера и его непривязанности"]
        provision["Provisioning Coordinator<br/>[Component]<br/>Регистрация устройств комплекта в реестре"]
        regclient["Registry Client<br/>[Component: REST]<br/>Обращение к реестру устройств"]
        images["Image Manager<br/>[Component]<br/>Изображения комплектов"]
        repo["Catalog Repository<br/>[Component]<br/>Комплекты, заказы, серийные номера"]
    end

    pg[("PostgreSQL<br/>[Database]")]
    minio[("MinIO<br/>[Object Storage]")]

    gwext -->|"REST, витрина"| catapi
    gwext -->|"REST, заказы"| orderapi
    gwext -->|"REST, активация"| actapi

    catapi --> kitmgr
    catapi --> images
    orderapi --> ordermgr
    actapi --> serial
    serial --> provision
    provision --> regclient
    regclient -->|"REST, регистрация устройств"| dregext
    kitmgr --> repo
    ordermgr --> repo
    serial --> repo
    provision --> repo
    images -.-> minio
    repo -.-> pg

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class catapi,orderapi,actapi,kitmgr,ordermgr,serial,provision,regclient,images,repo comp
    class gwext,dregext ext
    class pg,minio store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| Catalog API | Витрина готовых комплектов с описанием и составом |
| Order API | Оформление заказа и его текущий статус |
| Activation API | Активация купленного комплекта по серийным номерам |
| Kit Manager | Состав комплекта: какие устройства и хаб в него входят |
| Order Manager | Жизненный цикл заказа от оформления до выдачи |
| Serial Validator | Проверка серийного номера: существует, принадлежит комплекту, ещё не привязан |
| Provisioning Coordinator | Доведение устройств комплекта до реестра, чтобы они появились у пользователя |
| Registry Client | Обращение к Device Registry при активации |
| Image Manager | Изображения комплектов в объектном хранилище |
| Catalog Repository | Комплекты, заказы и серийные номера |

Этот сервис закрывает требование о продаже готовых комплектов и делает связным путь от коробки в магазине до работающего устройства: заказ, активация серийников, появление устройств в реестре, подключение через хаб.

Активация затрагивает два сервиса, поэтому при неуспешной регистрации в реестре комплект должен остаться неактивированным — это место для саги с компенсацией.
