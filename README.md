# My Market App

Мультипроект «Витрина интернет-магазина»: основное веб-приложение на Spring
WebFlux + Thymeleaf с кешем товаров в Redis, отдельный реактивный сервис
платежей и сервер авторизации OAuth2. Покупатель авторизуется в витрине по
логину/паролю (Spring Security), а сама витрина авторизуется в сервисе
платежей по протоколу OAuth2 Client Credentials.

## Модули

- **`my-market-app`** — витрина товаров, корзина, заказы.
  - Читает/пишет товары, корзину и заказы через Spring Data R2DBC
    (PostgreSQL); кеширует список товаров и карточки товара в Redis.
  - Авторизация покупателя по логину/паролю (Spring Security, форма логина
    по умолчанию), данные пользователей — в таблице `users` (пароли в виде
    BCrypt-хешей). Корзина и список заказов привязаны к авторизованному
    пользователю.
  - Анонимный пользователь видит только витрину товаров и карточку товара;
    корзина, заказы и оформление покупки доступны только авторизованным —
    ограничение работает и на уровне HTML (кнопки/ссылки скрыты), и на
    уровне маршрутов контроллеров.
  - Ходит в `payment-service` за балансом и списанием средств при
    оформлении заказа, получая перед каждым запросом токен доступа у
    `auth-server` по Client Credentials Flow.
- **`payment-service`** — реактивный REST-сервис с двумя эндпоинтами:
  получение баланса счёта и списание суммы заказа. Баланс хранится в памяти
  сервиса отдельно для каждого покупателя (идентифицируется по имени
  пользователя, передаваемому в запросе). Принимает запросы только от
  клиентов, авторизованных в `auth-server` (OAuth2 Resource Server, JWT).
- **`auth-server`** — сервер авторизации OAuth2
  (`spring-boot-starter-oauth2-authorization-server`). Единственный
  зарегистрированный клиент — `my-market-app`, которому выдаются токены по
  Client Credentials Flow со scope `payment.access` для доступа к
  `payment-service`.

Все три модуля собираются одним Gradle-мультипроектом.

## Технологии

- **Java 21** — основной язык
- **Spring Boot 3.2** — автоконфигурация, встроенный Netty
- **Spring WebFlux** — реактивные контроллеры в `my-market-app` и
  `payment-service`
- **Spring Security** — форма логина в `my-market-app` (пользователи в БД),
  OAuth2 Client (`my-market-app`) и OAuth2 Resource Server
  (`payment-service`) для Client Credentials Flow
- **Spring Authorization Server** — `auth-server`
- **Thymeleaf** — шаблонизатор
- **Spring Data R2DBC** + **PostgreSQL** — хранилище товаров/корзины/заказов/
  пользователей в `my-market-app`
- **Spring Data Redis (Reactive)** — кеш товаров в `my-market-app`
- **OpenAPI Generator** — генерация WebClient-клиента (`my-market-app`) и
  серверных интерфейсов контроллера (`payment-service`) из
  (`/payment-service/src/main/resources/openapi/payment-api.yaml`)
- **JUnit 5 + Mockito + Reactor Test + Spring Security Test** — тестирование
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

Тесты `my-market-app` и `payment-service` не требуют внешних сервисов:
R2DBC-тесты используют H2 в режиме PostgreSQL, кеш — `embedded-redis`,
OAuth2-авторизация в тестах мокается через `spring-security-test`
(`mockUser()`/`mockJwt()`), поэтому реальные `auth-server`/`payment-service`
для прогона тестов поднимать не нужно.

### Docker Compose

Поднимает Postgres, Redis, `auth-server`, `payment-service` и
`my-market-app` одной командой (схема БД, включая тестовых пользователей,
накатывается автоматически при первом старте контейнера Postgres):

```bash
docker compose up --build
```

После запуска витрина доступна на `http://localhost:8080`.

## Авторизация покупателя

Витрина использует стандартную форму логина Spring Security (`/login`).
В схему БД (`my-market-app/src/main/resources/db/schema.sql`) предзагружены
два тестовых покупателя:

| Логин  | Пароль   |
|--------|----------|
| buyer1 | password |
| buyer2 | password |

Каждый из них видит только свою корзину и свои заказы — при оформлении
заказа платёж списывается со счёта именно этого покупателя в
`payment-service`.
