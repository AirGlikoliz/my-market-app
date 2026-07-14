# My Market App

Мультипроект «Витрина интернет-магазина»: основное веб-приложение на Spring
WebFlux + Thymeleaf с кешем товаров в Redis и отдельный реактивный сервис
платежей, интеграция между которыми построена на клиенте/сервере,
сгенерированных из общей OpenAPI-спецификации.

## Модули

- **`my-market-app`** — витрина товаров, корзина, заказы. Читает/пишет товары
  через Spring Data R2DBC (PostgreSQL), кеширует список товаров и карточки
  товара в Redis, ходит в `payment-service` за балансом и списанием средств
  при оформлении заказа.
- **`payment-service`** — реактивный REST-сервис на Spring WebFlux с двумя
  эндпоинтами: получение баланса счёта и списание суммы заказа. Баланс —
  фиксированное значение из конфигурации, хранится в памяти сервиса.

Оба модуля собираются одним Gradle-мультипроектом.

## Технологии

- **Java 21** — основной язык
- **Spring Boot 3.2** — автоконфигурация, встроенный Netty
- **Spring WebFlux** — реактивные контроллеры в обоих модулях
- **Thymeleaf** — шаблонизатор
- **Spring Data R2DBC** + **PostgreSQL** — хранилище товаров/заказов `my-market-app`
- **Spring Data Redis (Reactive)** — кеш товаров в `my-market-app`
- **OpenAPI Generator** — генерация WebClient-клиента (`my-market-app`) и
  серверных интерфейсов контроллера (`payment-service`) из
  (`/payment-service/src/main/resources/openapi/payment-api.yaml`)
- **JUnit 5 + Mockito + Reactor Test** — тестирование
- **Gradle** — сборка

## Требования

- JDK 21
- PostgreSQL 15+ (для `my-market-app`)
- Redis 6+ (для `my-market-app` и кеша товаров)
- Docker и Docker Compose — опционально, для контейнерного запуска

## Сборка мультипроекта

```bash
./gradlew build
```

## Запуск тестов
```bash
./gradlew test
```

### Docker Compose

Поднимает Postgres, Redis, `payment-service` и `my-market-app` одной
командой (схема БД накатывается автоматически при первом старте контейнера
Postgres):

```bash
docker compose up --build
```
