# Shorts Blocker Kids Website

Static production website source for `https://shortsblockerkids.de`.

## Commands

```bash
npm run check
npm run build
npm run smoke
```

The build output is written to `website/dist/`.

## Preview Artifacts

Preview screenshots under `website/preview/` are intentionally tracked as
visual review evidence for public website revisions. Generated production build
output stays untracked under `website/dist/`.

## Release State

The current public install CTA is `Coming soon on Google Play` because no live
Google Play or beta testing URL is configured in `src/config.mjs`.

If a live Play URL becomes available, set `playStoreUrl` in `src/config.mjs`
and rebuild. If a closed or open testing URL becomes available before the
public listing, set `betaTestingUrl` instead.

## Metrics Widget

The metrics widget ships without an endpoint and shows neutral private-testing
copy. If a metrics endpoint is added later, it must return verified real data:

```json
{
  "verified": true,
  "privateTestingFamilies": 0,
  "blockedEvents": 0,
  "generatedAt": "2026-05-31T00:00:00Z"
}
```

The website must not use fake install, user, or blocking numbers.

## Незалежне оновлення сайту

Станом на 2026-10-13 чинний контейнер використовує `nginx:1.27-alpine` з
монтуванням `/var/www/shortsblockerkids.de` тільки для читання. Поточна збірка
джерел не збігається з п'ятьма із 11 файлів публічного сайту; її розгортання
змінило б вміст. Перехід на образ без зміни публічного вмісту потребує окремого
образу з точних 11 чинних файлів, резервної копії каталогу сайту й Compose та
перевірки SHA кожного файлу в образі перед заміною контейнера.

Для наступного релізу зафіксувати чистий коміт джерел і точний перелік
погоджених змін вмісту.
У `website/` виконати `npm run check` і `npm run smoke`; остання команда створює
`dist/`. Записати список і SHA-256 усіх файлів `dist/`, перевірити відсутність
сторонніх файлів і символічних посилань. `website/Dockerfile` копіює тільки
`dist/` у порожній каталог nginx. Для поточної структури з 11 файлами з кореня
репозиторію сформувати маніфест у Bash: сортувати шляхи байтово через NUL з
`LC_ALL=C`, прибрати початкове `./`, для кожного файлу вивести `sha256sum`.
Кожен рядок маніфесту має вигляд `SHA256␠␠відносний/шлях` і закінчується LF;
SHA-256 цих рядків і є `TREE_SHA` (без BOM і CRLF).
Для чинних 11 файлів публічного каталогу той самий алгоритм дає
`1d8733ac42eb0da14431d6f84b54015d7ba57d2c1fe34ee46b7ba7f7c6088803`;
це контрольна сума першого переходу, а не поточної збірки джерел.

```bash
set -euo pipefail
export LC_ALL=C
COMMIT="$(git rev-parse HEAD)"
manifest() (
  cd website/dist
  find . -type f -print0 | sort -z |
    while IFS= read -r -d '' file; do sha256sum "${file#./}"; done
)
manifest
TREE_SHA="$(manifest | sha256sum | cut -d ' ' -f1)"
docker build --pull=false -f website/Dockerfile \
  -t "shorts-blocker-kids-website:sha-${COMMIT}-tree-${TREE_SHA}" website
```

Зберегти в записі релізу точний вивід `manifest`, `COMMIT` і `TREE_SHA`. До
оновлення перевірити, що дерево файлів і SHA в образі точно збігаються з
погодженим артефактом; записати ідентифікатор образу.

Перед першим переходом обов'язково зберегти точні копії чинного файлу Compose
і всіх 11 файлів каталогу сайту, їхні SHA та ізольовано перевірити копії:
чинний nginx образ не містить монтуваний публічний вміст. Для наступних релізів
зберігати попередній ідентифікатор/тег образу та файл Compose. Після окремого
погодження змінити
тільки `website.image` у `/opt/shorts-blocker-kids/docker-compose.yml` та
адресно відтворити тільки `website` з `--no-deps --no-build`. Перший перехід з
монтування також вилучає лише монтування каталогу сайту з цього сервісу; сам
каталог залишається для повернення. Мережу, псевдонім, перевірку здоров'я й
Caddy не змінювати.
Перевірити `/`, `/privacy`, `/support`, `/terms`, маршрут `www`, стан здоров'я
контейнера, ідентифікатори, образи, перезапуски й монтування інших 16
контейнерів, сім активних таймерів і маршрути сусідів. Для повернення відновити
лише попередні `website.image` і, якщо було, монтування каталогу сайту, не
змінюючи решту файлу Compose; адресно відтворити тільки `website` і повторити
ті самі перевірки.

Перед поданням у Play перевірити HTTPS-сертифікат і публічні сторінки в браузері.
Сторінка приватності має відкриватися без входу, географічних обмежень і вимоги
PDF; підтримка показує `svalerii535@gmail.com`. Звірити дані релізу, SDK та
розкриття Device Admin з фактично завантаженим AAB. Поки немає погодженого URL
Play або тестування, залишати `Coming soon on Google Play`; не додавати
непідтверджені числа чи пряме основне посилання на APK. Нова кінцева точка метрик
потребує окремого погодження й підтверджених даних.

[Історичні відомості про розгортання](../docs/SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md)
не підтверджують поточну доступність.
