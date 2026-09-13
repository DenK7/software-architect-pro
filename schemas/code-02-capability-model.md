# Диаграмма кода — модель возможностей устройств

Ключевое архитектурное решение всей работы. Устройство описывается не типом, а набором возможностей: «включить свет» и «отпереть ворота» — это одна и та же команда над разными возможностями, а не два сервиса. Подключение нового типа устройства партнёра сводится к записи в справочнике и не требует релиза.

```mermaid
%% C4 Level 4 — Code. Модель возможностей
classDiagram
    class DeviceType {
        <<Reference Data>>
        +UUID id
        +String code
        +String manufacturer
        +String model
        +List~CapabilitySpec~ capabilities
    }
    class CapabilitySpec {
        <<Reference Data>>
        +CapabilityKind kind
        +Metric metric
        +Unit unit
        +BigDecimal minValue
        +BigDecimal maxValue
        +boolean readOnly
    }
    class CapabilityKind {
        <<enumeration>>
        OnOff
        Level
        Setpoint
        Lock
        Sensor
        Stream
    }
    class Metric {
        <<Reference Data>>
        +String code
        +String title
        +ValueKind valueKind
    }
    class Unit {
        <<Reference Data>>
        +String code
        +String symbol
        +Unit baseUnit
        +BigDecimal factor
    }
    class ValueKind {
        <<enumeration>>
        Numeric
        Boolean
        Text
    }

    class Device {
        <<Device Registry>>
        +UUID id
        +String serialNumber
        +UUID homeId
        +UUID typeId
        +String name
        +DeviceStatus status
        +Instant lastSeenAt
    }
    class DeviceStatus {
        <<enumeration>>
        Registered
        Active
        Offline
        Decommissioned
    }
    class CapabilityResolver {
        <<Device Registry>>
        +resolve(Device) Set~CapabilitySpec~
        +supports(Device, CapabilityKind) boolean
    }

    class Command {
        <<Device Control>>
        +UUID id
        +UUID deviceId
        +CapabilityKind capability
        +String action
        +Map~String,Object~ params
        +Duration timeToLive
        +CommandStatus status
    }
    class CommandValidator {
        <<Device Control>>
        +validate(Command, Set~CapabilitySpec~) Result
    }

    DeviceType "1" *-- "many" CapabilitySpec : состав возможностей
    CapabilitySpec --> CapabilityKind : вид
    CapabilitySpec --> Metric : для вида Sensor
    CapabilitySpec --> Unit : единица измерения
    Metric --> ValueKind : тип значения
    Unit --> Unit : приведение к базовой
    Device --> DeviceType : экземпляр типа
    Device --> DeviceStatus : состояние
    CapabilityResolver ..> Device : читает
    CapabilityResolver ..> DeviceType : через локальную проекцию справочника
    CommandValidator ..> CapabilityResolver : запрашивает возможности
    CommandValidator ..> Command : проверяет
    Command --> CapabilityKind : над какой возможностью
```

## Как это работает

Справочник владеет тем, **что вообще бывает**: типы устройств, их возможности, метрики и единицы. Реестр владеет тем, **что есть у конкретного пользователя**: экземпляр устройства со ссылкой на тип. Device Control владеет **командой** и проверяет её допустимость через возможности.

Проверка команды выглядит так: `CommandValidator` берёт у `CapabilityResolver` набор `CapabilitySpec` для устройства и убеждается, что среди них есть нужный `CapabilityKind`, а параметры укладываются в заданный диапазон. Запереть лампу не получится — у её типа нет возможности `Lock`.

**Почему возможности лежат в справочнике, а не в коде.** Добавление нового типа устройства — это строка в `DeviceType` и несколько строк в `CapabilitySpec`. Ни реестр, ни Device Control при этом не меняются и не пересобираются. Именно так закрывается требование ТЗ про «будущее неуточнённое поведение» и подключение партнёрских устройств.

**Почему `Unit` ссылается сам на себя.** Единицы приводятся к базовой через множитель: чтобы агрегаты по температуре считались корректно, показания в разных шкалах надо привести к одной. Этим занимается Unit Normalizer в телеметрии.

**Почему у метрики есть `ValueKind`.** Не всякое измерение — число: датчик движения отдаёт булево значение, геркон — состояние «открыто или закрыто». Значения хранятся отдельными колонками по типу, без универсального JSON-поля, чтобы по числовым рядам можно было дёшево считать агрегаты.
