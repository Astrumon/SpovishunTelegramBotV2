# SpovishunTelegramBotV2

Telegram-бот для групових згадок у чаті. Додай учасників у іменовані групи і тегай усіх однією командою `/ping`.

## Запуск

Створи `.env` у корені проекту:
```env
TELEGRAM_BOT_TOKEN=your_telegram_bot_token
ADMINS=123456789,987654321
DB_URL=jdbc:postgresql://localhost:5432/spovishun
DB_USER=postgres
DB_PASSWORD=secret
```
```bash
# Dev-режим (in-memory, без БД)
./gradlew runDev

# Prod-режим (PostgreSQL)
./gradlew runProd
```

## Команди

| Команда | Опис |
|---|---|
| `/start` | Реєстрація та привітання |
| `/register` | Ручна реєстрація |
| `/all [текст]` | Тегнути всіх учасників |
| `/ping <група> [текст]` | Тегнути всіх учасників групи |
| `/groups` | Список усіх груп |
| `/members` | Список усіх учасників |
| `/newgroup <назва>` | Створити групу *(адмін)* |
| `/delgroup <назва>` | Видалити групу *(адмін)* |
| `/addtogroup <група> @user` | Додати до групи *(адмін)* |
| `/removefromgroup <група> @user` | Видалити з групи *(адмін)* |

## Стек

Kotlin · Exposed · PostgreSQL · Koin · Ktor · Coroutines
