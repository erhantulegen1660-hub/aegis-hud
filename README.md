# AegisHud 1.1

Анимированный боковой скорборд для Paper 1.21.11: ник, пинг, аметисты, живые, убийства, зона.

## Сборка
Нужны Java 21 и Maven.

    mvn clean package

Готовый файл: `target/AegisHud.jar`, кидай в `plugins/` и перезапусти сервер.

## Команды
- `/aegishud reload`: перезагрузить config.yml (aegishud.admin)
- `/aegishud toggle`: включить или выключить свой скорборд
- `/aegishud resetstats`: сбросить убийства и смерти (aegishud.admin)
- `/amethyst`: свой баланс аметистов
- `/amethyst balance <ник>`, `give|take|set <ник> <число>`: управление балансом (aegishud.admin)

## Аметисты
По умолчанию используется встроенный баланс (файл `plugins/AegisHud/balances.yml`).
Если у тебя уже есть плагин экономики, поставь PlaceholderAPI и в `config.yml` укажи
`currency.source: "%плейсхолдер_баланса%"`.
