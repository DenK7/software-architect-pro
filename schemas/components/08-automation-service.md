# Диаграмма компонентов — Automation Service

```mermaid
%% C4 Level 3 — Components. Automation Service
flowchart TB
    gwext["API Gateway<br/>[Container]"]
    dctrlext["Device Control<br/>[Container]"]
    dregext["Device Registry<br/>[Container]"]
    homeext["Home Service<br/>[Container]"]

    subgraph as["Automation Service [Container: Java / Spring Boot]"]
        ruleapi["Scenario API<br/>[Component: REST Controller]<br/>Создание, изменение, включение сценариев"]
        ruleval["Scenario Validator<br/>[Component]<br/>Проверка триггеров и действий по возможностям"]
        regclient["Registry Client<br/>[Component: REST]<br/>Устройства и их возможности"]
        evtsub["Event Consumer<br/>[Component]<br/>Приём измерений и событий устройств"]
        matcher["Trigger Matcher<br/>[Component]<br/>Подбор сценариев под пришедшее событие"]
        cond["Condition Evaluator<br/>[Component]<br/>Проверка условий: время, состояние, пороги"]
        exec["Action Executor<br/>[Component]<br/>Выполнение действий сценария"]
        cmdclient["Command Client<br/>[Component: REST]<br/>Отправка команды устройству"]
        sched["Scheduler<br/>[Component: Quartz, JDBC JobStore]<br/>Триггеры по времени в часовом поясе дома,<br/>одно срабатывание на кластер"]
        homeclient["Home Client<br/>[Component: REST]<br/>Часовой пояс дома"]
        repo["Scenario Repository<br/>[Component]<br/>Сценарии и журнал срабатываний"]
        evtpub["Event Publisher<br/>[Component: Outbox]<br/>Событие о срабатывании сценария"]
    end

    pg[("PostgreSQL<br/>[Database]")]
    mq[("RabbitMQ<br/>[Message Broker]")]

    gwext -->|"REST, сценарии"| ruleapi
    mq ==>|"измерения, события устройств"| evtsub

    ruleapi --> ruleval
    ruleval --> regclient
    regclient -->|REST| dregext
    ruleapi --> homeclient
    homeclient -->|"REST, часовой пояс"| homeext
    ruleapi --> repo

    evtsub --> matcher
    sched --> repo
    sched --> matcher
    matcher --> repo
    matcher --> cond
    cond --> exec
    exec --> cmdclient
    exec --> evtpub
    exec --> repo
    cmdclient -->|"REST, команда"| dctrlext

    repo -.-> pg
    evtpub -.-> pg
    evtpub ==>|"сценарий сработал"| mq

    classDef comp fill:#85bbf0,stroke:#5d82a8,color:#1c2230
    classDef ext fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef store fill:#438dd5,stroke:#2e6295,color:#ffffff
    class ruleapi,ruleval,regclient,evtsub,matcher,cond,exec,cmdclient,sched,homeclient,repo,evtpub comp
    class gwext,dctrlext,dregext,homeext ext
    class pg,mq store
```

## Ответственности компонентов

| Компонент | Ответственность |
|---|---|
| Scenario API | Создание, изменение, включение и выключение пользовательских сценариев |
| Scenario Validator | Проверка того, что триггер и действие вообще применимы к выбранным устройствам |
| Registry Client | Получение списка устройств и их возможностей при составлении сценария |
| Event Consumer | Приём измерений и событий устройств из брокера |
| Trigger Matcher | Подбор сценариев, чей триггер совпал с пришедшим событием |
| Condition Evaluator | Проверка условий: время суток, состояние других устройств, пороги значений |
| Action Executor | Выполнение действий сработавшего сценария |
| Command Client | Синхронная отправка команды в Device Control — команде нужен ответ |
| Scheduler | Триггеры по времени: расписания, задержки, повторы. Quartz с хранением расписаний в собственной базе — срабатывание одно на весь кластер, пропущенные запуски догоняются после простоя |
| Home Client | Получение часового пояса дома при создании и изменении сценария |
| Scenario Repository | Сценарии, их расписания и журнал срабатываний |
| Event Publisher | Публикация факта срабатывания, на которую подписан Notification |

Команда уходит синхронным вызовом, а не событием: пользователю нужно знать, выполнилось ли действие. Само срабатывание при этом публикуется событием — на него реагируют уведомления, и им ответ не нужен.

Планировщик обязан быть кластерным. Сервис работает в нескольких репликах, и обычный планировщик Spring сработал бы на каждой: сценарий «в 22:00 включить свет» выполнился бы столько раз, сколько поднято реплик. Quartz с хранением расписаний в PostgreSQL берёт блокировку и гарантирует одно срабатывание, а заодно умеет догонять запуски, пропущенные во время простоя.

Время в расписании — время дома, а не сервера. «В 22:00» для домов в разных часовых поясах наступает в разные моменты, поэтому при создании и изменении сценария Automation запрашивает часовой пояс у Home Service и сохраняет его вместе со сценарием. Ходить за ним на каждое срабатывание не нужно — часовой пояс дома меняется редко, но при его смене сценарии дома придётся пересчитать.

Все сценарии исполняются в облаке. Задержка при этом невелика: длинный опрос доставляет команду хабу практически мгновенно. Но зависимость от связи прямая — при обрыве интернета автоматика не работает, дом остаётся управляемым только вручную. Это осознанное ограничение текущего этапа, снимается переносом простых правил на хаб.
