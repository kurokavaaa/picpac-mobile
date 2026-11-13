# PicPac Studio — бэкенд (Kotlin + Ktor)

Сервер для мобильного приложения PicPac: регистрация с капчей, личный кабинет, проекты с фото,
проверка фото NSFW-моделью, заказ только после регистрации, производство только после оплаты,
пакет для печати (ZIP + штрихкоды листов), статусы как на WB/Ozon и заглушка ИИ-чата.

> Прототип для хакатона. Оплата, SMS/email и ИИ-чат — заглушки. Цены взяты с super.picpac.by
> 11.11.2026; цены форматов, которых нет на сайте, условные (`approximate: true`).

## Запуск

Нужна Java 21+. Подойдёт JDK, встроенный в Android Studio:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
cd backend
./gradlew run
```

Сервер стартует на `http://localhost:8080`.

**Для показа** запускайте с демо-режимом — он включает кнопку «следующий этап» (`/demo/...`):

```bash
DEMO_MODE=true ./gradlew run
```

По умолчанию демо-режим выключен: через него владелец заказа мог бы сам «одобрить» свои фото на модерации. Проверка: `curl http://localhost:8080/health`.

Тесты:

```bash
./gradlew test
```

### Переменные окружения

| Переменная | Что делает | По умолчанию |
|---|---|---|
| `PORT` | порт сервера | `8080` |
| `DATA_DIR` | где лежат база, фото и пакеты | `data` |
| `JWT_SECRET` | секрет для токенов входа. Без него генерируется случайный, и после перезапуска все входят заново | случайный |
| `NSFW_MODEL_DIR` | папка с NSFW-моделью (см. ниже). Без неё все фото уходят на ручную проверку | не задано |
| `NSFW_REVIEW_THRESHOLD` | порог вероятности NSFW, с которого фото идёт на ручную проверку | `0.5` |
| `DEMO_MODE` | `true` включает `/demo/*` (для показа) | `false` |

### NSFW-модель

Используется [Falconsai/nsfw_image_detection](https://huggingface.co/Falconsai/nsfw_image_detection)
(лицензия Apache 2.0) в формате ONNX из
[onnx-community/nsfw_image_detection-ONNX](https://huggingface.co/onnx-community/nsfw_image_detection-ONNX).
Модель работает локально через ONNX Runtime: без интернета, ключей и оплаты.

Скачать в папку `models/nsfw` (в git не попадает):

```bash
mkdir -p ../models/nsfw/onnx && cd ../models/nsfw && for f in config.json preprocessor_config.json onnx/model_quantized.onnx; do curl -L -o "$f" "https://huggingface.co/onnx-community/nsfw_image_detection-ONNX/resolve/main/$f"; done
```

`model_quantized.onnx` весит ~87 МБ. Полная `onnx/model.onnx` (~343 МБ) чуть точнее.

```bash
NSFW_MODEL_DIR=../models/nsfw ./gradlew run
```

Подозрительное фото **не блокируется**, а отправляется сотруднику: у модели бывают ложные
срабатывания (пляж, дети в купальниках). Модель ищет только откровенный контент — оружие,
символику и запрещённый текст она не видит.

## Как устроен заказ

```
регистрация (капча) → проект → фото (автопроверка) → заказ (AWAITING_PAYMENT)
  → оплата → PAID → [ON_REVIEW, если есть фото на проверке] → PACKED (собран ZIP)
  → PRINTING → QUALITY_CHECK → SHIPPED (трек-номер) → DELIVERED
```

- Заказ без входа невозможен (401), в производство без оплаты — тоже.
- **Без дублей.** `POST /orders` требует заголовок `Idempotency-Key`. Повтор с тем же ключом вернёт тот же заказ (200 вместо 201). Повторная оплата не списывает деньги второй раз.
- **Сбой оплаты.** `{"outcome":"decline"}` имитирует отказ банка: заказ сохраняется, можно оплатить ещё раз.
- **Пакет для печати.** `data/packages/PP-000001.zip`: фото + `manifest.json` (продукт, формат, страница и штрихкод каждого листа).
- **Аудит.** Каждое действие пишется в таблицу `audit_log`: кто, когда, что, с каким объектом, результат.
- **Уведомления.** Есть email — пишем на email, нет — SMS на телефон. Это заглушка: сообщения видны в `GET /me/notifications`.

## API

Ответы — JSON. Ошибка всегда выглядит так: `{"error": {"code": "...", "message": "понятный текст"}}`.
Деньги — в копейках (`8700` = 87,00 BYN), для показа есть поле `totalDisplay`.

| Метод | Путь | Вход нужен | Что делает |
|---|---|---|---|
| GET | `/health` | — | проверка сервера и режима модерации |
| GET | `/auth/captcha` | — | капча: `{captchaId, question}` |
| POST | `/auth/register` | — | `{email?, phone?, password, captchaId, captchaAnswer}` → токен |
| POST | `/auth/login` | — | `{login, password}`, login — email или телефон |
| GET | `/pricing/catalog` | — | продукты, форматы, доставка |
| POST | `/pricing/quote` | — | расчёт цены `{product, format, pages?, quantity?, delivery?}` |
| POST | `/chat` | — | ИИ-помощник (заглушка) `{message}` |
| POST | `/chat/handoff` | да | позвать оператора |
| GET | `/me` | да | профиль, канал уведомлений, `suggestLinkEmail` |
| PUT | `/me/email` | да | привязать email `{email}` |
| GET | `/me/notifications` | да | история уведомлений |
| GET | `/me/orders` | да | заказы пользователя |
| POST | `/projects` | да | `{product, format, pages?, title?}` |
| GET | `/projects`, `/projects/{id}` | да | проекты и фото с вердиктом модерации |
| POST | `/projects/{id}/photos` | да | загрузка фото: multipart, поле `file`, JPEG/PNG до 20 МБ |
| POST | `/orders` | да | `{projectId, deliveryMethod, address}` + заголовок `Idempotency-Key` |
| GET | `/orders`, `/orders/{id}` | да | заказы с историей статусов |
| POST | `/orders/{id}/pay` | да | мок-оплата, `{"outcome":"decline"}` — отказ |
| GET | `/orders/{id}/package` | да | ZIP для производства (после оплаты) |
| POST | `/demo/orders/{id}/advance` | да | демо: следующий этап производства |

Токен передаётся в заголовке `Authorization: Bearer <token>`.

## Структура

```
src/main/kotlin/by/picpac/studio/
  Application.kt   — сборка сервисов и маршруты
  auth/            — регистрация, вход, капча, JWT, хеш паролей
  account/         — личный кабинет
  pricing/         — каталог и расчёт цены
  photos/          — проекты, загрузка и хранение фото
  moderation/      — NSFW-проверка (ONNX) и ручной режим
  orders/          — заказы, оплата, статусы, идемпотентность
  packaging/       — сборка ZIP для печати
  notifications/   — уведомления email/SMS (заглушка)
  chat/            — заглушка ИИ-помощника
  audit/           — журнал действий
  db/              — SQLite через JDBC, схема
```
