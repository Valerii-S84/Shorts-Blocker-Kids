# Shorts Blocker Kids — детальна дорожня карта реалізації

**Версія:** 1.1
**Дата:** 22 травня 2026
**Продукт:** Shorts Blocker Kids  
**Формат:** Android-додаток, local-only, без акаунта, без власного сервера  
**Головна функція:** блокувати YouTube Shorts на телефоні дитини через PIN батьків і локальні правила

---

## 0. Коротке рішення по продукту

Shorts Blocker Kids будується як **окремий простий Android-додаток**.

Головна формула:

```text
Встановив → ввів PIN → увімкнув захист → Shorts заблоковано
```

Продукт не повинен перетворюватися на велику систему батьківського контролю. Його сила — в одній чіткій функції:

```text
блокувати YouTube Shorts
```

---

## 1. Непорушні межі продукту

Ці правила не можна ламати під час розробки.

### 1.1. Що додаток робить

```text
Shorts Blocker Kids:
- працює на телефоні дитини;
- дає батькам створити PIN;
- дозволяє увімкнути або вимкнути захист;
- визначає відкриття YouTube Shorts;
- блокує Shorts;
- показує простий блокуючий екран;
- дозволяє тимчасовий доступ тільки через PIN;
- зберігає правила локально на пристрої.
```

### 1.2. Що додаток не робить

```text
На першій версії додаток НЕ робить:
- акаунти;
- власний сервер;
- cloud-sync;
- сімейні профілі;
- веб-кабінет;
- історію переглядів;
- аналіз відео;
- аналіз звуку;
- аналіз мови;
- запис екрана;
- читання повідомлень;
- моніторинг усіх додатків;
- прихований контроль;
- прихований збір даних;
- агресивний anti-uninstall;
- обхід системних обмежень Android.
```

### 1.3. Основна технічна межа

Додаток має бути чесним:

```text
Ми можемо блокувати Shorts, коли Android дає додатку потрібний Accessibility-доступ.

Ми не можемо гарантувати абсолютний захист, якщо дитина або інший користувач має повний доступ до системних налаштувань і вимикає Accessibility Service або видаляє додаток.

Перший реліз не повинен приховувати це від батьків.
```

---

# Roadmap

## Active Production Execution Lock

**Статус зараз:** ACTIVE
**Мета:** довести поточний продукт до production, monetization і Play Store без scope drift.

До завершення цього execution track не впроваджувати інші продуктові напрями.

Заборонено додавати до повного завершення production-track:

```text
- нові платформи;
- блокування інших сервісів, крім YouTube Shorts;
- analytics;
- ads;
- child profiles;
- family accounts;
- cloud sync;
- remote config;
- web dashboard;
- learning tasks;
- math challenges;
- schedules;
- TikTok/Reels blocking;
- device-owner strong mode;
- aggressive anti-uninstall;
- будь-які фічі, які не потрібні для Play Store production release.
```

Дозволено робити тільки те, що прямо закриває production-track:

```text
- policy package;
- Accessibility disclosure і consent;
- Play Accessibility declaration;
- review demo video;
- Privacy Policy;
- Data Safety;
- target audience decision;
- real-device blocker QA;
- AAB/release/signing pipeline;
- Google Play Billing і entitlement;
- internal/closed testing;
- production access request;
- staged rollout;
- hotfix loop після release.
```

Активний порядок виконання:

```text
P0. Policy package і Play review readiness.
P1. Real-device blocker QA.
P2. AAB release pipeline і signing.
P3. Google Play Billing + entitlement.
P4. Store listing, Privacy Policy, Data Safety, declarations.
P5. Internal testing.
P6. Closed testing 14+ днів, якщо потрібно для account type.
P7. Production access request.
P8. Staged rollout.
P9. Maintenance / hotfix loop.
```

Done цього lock:

```text
- Play policy package повністю готовий;
- blocker пройшов real-device QA;
- monetization доведена до робочого entitlement;
- AAB accepted in Play Console testing track;
- closed testing / production access вимоги виконані;
- app вийшов у production або готовий до final rollout без critical blockers.
```

## Phase 0 — Product Lock

**Статус зараз:** DONE  
**Мета:** зафіксувати чисту візію, щоб не роздувати продукт.

### Scope

У цій фазі фіксуємо:

```text
Назва: Shorts Blocker Kids
Суть: блокувати YouTube Shorts
Режим: local-only
Авторизація: PIN батьків
Сервер: немає
Акаунт: немає
Головний екран: YouTube Shorts Protection ON/OFF
```

### Не входить у фазу

```text
- будь-які інші платформи;
- будь-який аналіз мови;
- будь-який cloud;
- будь-яка велика parental-control система.
```

### Done

Фаза завершена, якщо:

```text
- є коротка продуктова візія;
- є список того, що додаток робить;
- є список того, що додаток не робить;
- всі майбутні рішення перевіряються проти головної фрази:
  “Блокує YouTube Shorts на телефоні дитини. Без акаунта. Без сервера. Просто PIN батька і локальні правила.”
```

---

## Phase 1 — Technical Feasibility Spike

**Статус зараз:** TODO  
**Мета:** довести технічно, що додаток може стабільно визначати YouTube Shorts і реагувати.

### Scope

Потрібно зробити маленький технічний прототип, не фінальний додаток.

Прототип має:

```text
- створити Android AccessibilityService;
- слухати події тільки від YouTube;
- визначати, що активний додаток — com.google.android.youtube;
- читати accessibility-tree поточного екрана;
- знайти стабільні ознаки Shorts;
- виконати просту реакцію:
  - або повернути назад;
  - або показати тестовий блокуючий overlay.
```

### Технічна логіка

Початкова схема:

```text
AccessibilityEvent
→ перевірка packageName == com.google.android.youtube
→ перевірка eventType
→ getRootInActiveWindow()
→ scan AccessibilityNodeInfo tree
→ detect Shorts pattern
→ trigger block action
```

### Події, які треба перевірити

```text
- TYPE_WINDOW_STATE_CHANGED
- TYPE_WINDOW_CONTENT_CHANGED
- TYPE_VIEW_SCROLLED
- TYPE_WINDOWS_CHANGED
```

Не всі вони обовʼязково залишаться у фінальній версії. У spike треба перевірити, що реально працює на живих пристроях.

### Ознаки Shorts, які треба дослідити

```text
- viewIdResourceName, що містить shorts/reel;
- YouTube Shorts recycler/container/player елементи;
- структура fullscreen vertical video;
- активний Shorts tab;
- наявність елементів Shorts-екрана;
- зміна дерева після swipe;
- поведінка при відкритті Shorts з головної;
- поведінка при відкритті Shorts з пошуку;
- поведінка при відкритті Shorts з deep link.
```

### Важливе правило

Не покладатися тільки на текст `Shorts`, бо він може бути локалізований, прихований або змінений. Детектор має бути максимально незалежним від мови інтерфейсу.

### Не входить у фазу

```text
- дизайн;
- PIN;
- billing;
- subscription;
- Play Store;
- красивий onboarding;
- антиобхід;
- production-якість.
```

### Done

Фаза завершена, якщо:

```text
- прототип запускається на реальному Android-пристрої;
- AccessibilityService вмикається через системні налаштування;
- додаток бачить тільки YouTube-події або фільтрує всі інші;
- прототип визначає Shorts мінімум у 5 сценаріях:
  1. відкриття Shorts з нижньої вкладки YouTube;
  2. відкриття Shorts з головної стрічки;
  3. відкриття Shorts після swipe;
  4. повернення з Shorts назад;
  5. повторний вхід у Shorts;
- помилкове спрацювання на звичайному YouTube-відео не відбувається в базовому тесті;
- є список стабільних сигналів для Shorts;
- є список нестабільних сигналів, які не можна використовувати як єдину умову;
- є рішення: блокувати через overlay, back-action або комбіновано.
```

### Ризик

YouTube часто змінює інтерфейс. Тому детектор не повинен бути одним hardcoded ID. Має бути rule-engine з кількома сигналами.

---

## Phase 2 — Android Project Foundation

**Статус зараз:** DONE  
**Мета:** створити чисту Android-основу, яку не доведеться переписувати після прототипу.

### Рекомендований стек

```text
Language: Kotlin
UI: Jetpack Compose + Material 3
Architecture: MVVM / simple clean architecture
State: Kotlin Flow / StateFlow
Local settings: Jetpack DataStore
Billing: Google Play Billing Library
Min SDK: 26 або 28
Target SDK: актуальний Google Play target, зараз орієнтир — Android 15 / API 35+
Backend: немає
Account: немає
Analytics: немає на першому релізі
```

### Чому не Flutter

Для цього продукту краще Android native, бо ядро — системний AccessibilityService. Native Kotlin дає менше обхідних шарів, простіше debug-ити accessibility-tree і легше пройти policy-review.

### Структура пакетів

```text
app/
  MainActivity.kt

core/
  model/
    ProtectionState.kt
    ParentPinState.kt
    SubscriptionState.kt
    TemporaryAllowState.kt

  storage/
    SettingsStore.kt
    PinStore.kt
    EntitlementStore.kt

  security/
    PinHasher.kt
    PinValidator.kt
    RateLimiter.kt

  billing/
    BillingClientManager.kt
    SubscriptionRepository.kt
    EntitlementResolver.kt

feature/
  onboarding/
    WelcomeScreen.kt
    PinSetupScreen.kt
    AccessibilityDisclosureScreen.kt
    AccessibilityEnableScreen.kt

  dashboard/
    ProtectionDashboardScreen.kt

  pin/
    PinEntryScreen.kt
    ChangePinScreen.kt

  blocking/
    BlockOverlayController.kt
    BlockOverlayView.kt
    TemporaryAllowDialog.kt

accessibility/
  ShortsBlockerAccessibilityService.kt
  AccessibilityEventRouter.kt
  YouTubeShortsDetector.kt
  AccessibilityTreeScanner.kt
  DetectorRules.kt
  DetectorDebugSnapshot.kt
```

### Build quality

Налаштувати одразу:

```text
- Kotlin strict warnings where reasonable;
- detekt або ktlint;
- Gradle version catalog;
- release/debug build types;
- debug-only logs;
- no secrets in repo;
- signing config не в Git;
- CI для build + unit tests.
```

### Не входить у фазу

```text
- повний UI;
- billing;
- фінальний detector;
- Play Store release.
```

### Done

Фаза завершена, якщо:

```text
- проєкт відкривається в Android Studio;
- debug build збирається;
- release build збирається;
- є чиста пакетна структура;
- є CI-перевірка build/test;
- немає зайвих permissions;
- немає INTERNET permission без чіткої потреби;
- README пояснює, як зібрати і запустити додаток;
- код не копіює GPL-репозиторії без правової перевірки.
```

---

## Phase 3 — Core UX Skeleton

**Статус зараз:** DONE  
**Мета:** зробити простий інтерфейс, який батьки зрозуміють без інструкції.

### Екрани

#### 1. Welcome Screen

Текст:

```text
Shorts Blocker Kids
Блокує YouTube Shorts на телефоні дитини.
Без акаунта. Без сервера. Просто PIN батьків.
```

Кнопка:

```text
Почати
```

#### 2. PIN Setup Screen

Поля:

```text
Створіть PIN
Повторіть PIN
```

Правила:

```text
- 4–6 цифр;
- не дозволяти 0000, 1111, 1234, 123456;
- підтвердження PIN обовʼязкове;
- PIN не зберігати відкритим текстом.
```

#### 3. Accessibility Disclosure Screen

Це критично важливий екран.

Текст має бути простий і чесний:

```text
Shorts Blocker Kids використовує Android Accessibility Service, щоб визначити, коли на цьому телефоні відкрито YouTube Shorts, і показати блокуючий екран.

Додаток не читає повідомлення.
Не записує екран.
Не записує звук.
Не зберігає історію переглядів.
Не надсилає дані на власний сервер.
Правила працюють локально на цьому телефоні.
```

Кнопка:

```text
Я розумію і хочу увімкнути захист
```

#### 4. Enable Accessibility Screen

Показати:

```text
1. Натисніть “Відкрити налаштування”.
2. Знайдіть “Shorts Blocker Kids”.
3. Увімкніть сервіс.
4. Поверніться в додаток.
```

Кнопки:

```text
Відкрити налаштування
Я увімкнув
```

#### 5. Protection Dashboard

Головний екран:

```text
YouTube Shorts Protection
[ ON / OFF ]

Статус:
- PIN створено
- Accessibility Service увімкнено / вимкнено
- Захист активний / неактивний
- Підписка активна / trial / expired
```

#### 6. Block Overlay

Текст:

```text
Shorts заблоковано

Щоб вимкнути захист або дозволити доступ,
потрібен PIN батьків.
```

Кнопки:

```text
Вийти з Shorts
Ввести PIN
```

#### 7. Temporary Allow

Після PIN:

```text
Дозволити Shorts на:
- 5 хвилин
- 10 хвилин
- 15 хвилин
```

### Не входить у фазу

```text
- складні теми;
- гейміфікація;
- навчальні завдання;
- багато режимів;
- статистика;
- аналітика.
```

### Done

Фаза завершена, якщо:

```text
- батько може пройти onboarding від першого екрану до dashboard;
- PIN створюється;
- Accessibility disclosure показується до переходу в системні налаштування;
- dashboard чітко показує, чи захист активний;
- весь текст спокійний, короткий і без залякування;
- користувач розуміє, що додаток робить і чого не робить.
```

---

## Phase 4 — Local Storage + PIN Security

**Статус зараз:** DONE  
**Мета:** зробити локальні правила й PIN безпечно, без сервера.

### Що зберігати локально

```text
settings:
- protectionEnabled: Boolean
- accessibilityDisclosureAccepted: Boolean
- selectedMode: BLOCK_SHORTS
- temporaryAllowUntil: Timestamp?
- subscriptionStateCached: ACTIVE/TRIAL/GRACE/EXPIRED/UNKNOWN
- lastBillingCheckAt: Timestamp?

pin:
- pinHash
- pinSalt
- pinHashVersion
- failedAttempts
- lockoutUntil
```

### PIN

PIN не можна зберігати відкритим текстом.

Мінімальна реалізація:

```text
- генерувати salt;
- хешувати PIN через PBKDF2WithHmacSHA256;
- зберігати тільки hash + salt;
- додати rate limit після невдалих спроб.
```

Приклад rate limit:

```text
0–4 неправильні спроби: без блокування
5 неправильних спроб: 30 секунд
6 неправильних спроб: 60 секунд
7+ неправильних спроб: 5 хвилин
```

### DataStore

Для правил і станів використовувати DataStore.

```text
SettingsStore:
- readSettings(): Flow<AppSettings>
- setProtectionEnabled(Boolean)
- setDisclosureAccepted(Boolean)
- setTemporaryAllowUntil(Instant?)
- setCachedEntitlement(SubscriptionState)
```

### Не входить у фазу

```text
- cloud backup;
- sync;
- remote config;
- remote rules;
- історія переглядів.
```

### Done

Фаза завершена, якщо:

```text
- PIN ніколи не зберігається plain text;
- після перезапуску телефона правила зберігаються;
- після перезапуску додатку dashboard показує правильний стан;
- temporary allow автоматично закінчується;
- неправильний PIN має rate limit;
- unit tests покривають:
  - створення PIN;
  - перевірку PIN;
  - неправильний PIN;
  - lockout;
  - temporary allow expiry;
  - protectionEnabled state.
```

---

## Phase 5 — Accessibility Service Production Core

**Статус зараз:** DONE  
**Мета:** зробити production-ядро сервісу, яке працює подієво, локально і без зайвого збору даних.

### Маніфест

Потрібен service з permission:

```xml
android.permission.BIND_ACCESSIBILITY_SERVICE
```

Сервіс має мати metadata XML з обмеженим описом.

### Базові параметри сервісу

```text
packageNames:
- com.google.android.youtube

eventTypes:
- TYPE_WINDOW_STATE_CHANGED
- TYPE_WINDOW_CONTENT_CHANGED
- TYPE_VIEW_SCROLLED
- TYPE_WINDOWS_CHANGED

flags:
- FLAG_REPORT_VIEW_IDS
- FLAG_RETRIEVE_INTERACTIVE_WINDOWS

capability:
- canRetrieveWindowContent=true
```

### Основна логіка

```kotlin
onAccessibilityEvent(event):
    if protection disabled -> return
    if temporary allow active -> return
    if package != YouTube -> return
    if event type not relevant -> return
    root = getRootInActiveWindow() ?: return
    result = youtubeShortsDetector.detect(root, event)
    if result.isShortsHighConfidence:
        blockController.block()
```

### Performance

AccessibilityService не повинен сканувати дерево безконтрольно.

Правила:

```text
- немає polling;
- тільки event-driven;
- debounce 300–800 ms;
- cooldown після block action 1000–1500 ms;
- не сканувати, якщо protection OFF;
- не сканувати, якщо temporary allow активний;
- не логувати дерево в release build;
- debug snapshots тільки вручну і тільки в debug build.
```

### Не входить у фазу

```text
- повний Play Billing;
- фінальний Play Store текст;
- антиобхід;
- статистика.
```

### Done

Фаза завершена, якщо:

```text
- AccessibilityService стартує після системного дозволу;
- сервіс зупиняється, якщо дозвіл вимкнено;
- сервіс не обробляє сторонні додатки;
- сервіс не пише повний accessibility-tree у release logs;
- сервіс не збирає історію;
- сервіс не відправляє дані в мережу;
- detector викликається тільки для YouTube;
- є інтеграційний тест або manual test checklist для сервісу.
```

---

## Phase 6 — YouTube Shorts Detector v1

**Статус зараз:** DONE  
**Мета:** зробити детектор, який не залежить від мови, не ламається від одного ID і має контроль помилок.

### Detector API

```kotlin
interface ShortsDetector {
    fun detect(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent
    ): DetectionResult
}

data class DetectionResult(
    val isShorts: Boolean,
    val confidence: Confidence,
    val reasons: List<String>,
    val matchedSignals: List<String>
)

enum class Confidence {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}
```

### Сигнали

#### High confidence

```text
- YouTube package + fullscreen Shorts player/container;
- YouTube package + Shorts recycler/container ID;
- YouTube package + Shorts-specific player controls;
- YouTube package + vertical short-form viewer structure.
```

#### Medium confidence

```text
- YouTube package + active Shorts surface;
- YouTube package + view IDs/classes that often appear only in Shorts;
- YouTube package + repeated vertical feed pattern.
```

#### Low confidence

```text
- only visible text “Shorts”;
- only contentDescription;
- only bottom navigation tab;
- only URL/deep-link assumption.
```

### Blocking rule

```text
Block only HIGH confidence.
Optionally block MEDIUM confidence after repeated confirmation.
Never block LOW confidence alone.
```

### Debug mode

Для розробки потрібен debug-only інструмент:

```text
- кнопка “Capture YouTube UI Snapshot” тільки в debug build;
- зберігає локально sanitized дерево;
- не пише назви відео;
- не пише персональні тексти;
- не доступний у release.
```

Мета debug snapshot — знайти стабільні viewIdResourceName і структуру Shorts на різних пристроях.

### Не входить у фазу

```text
- аналіз контенту;
- аналіз мови;
- аналіз звуку;
- аналіз рекомендацій;
- історія відео.
```

### Done

Фаза завершена, якщо:

```text
- detector має мінімум 3 незалежні сигнали Shorts;
- detector не блокує звичайне YouTube-відео;
- detector не блокує YouTube search;
- detector не блокує YouTube home;
- detector не блокує subscriptions/library;
- detector блокує Shorts у мінімум 20 повторних тестах;
- false positive rate у manual test matrix нижче 1%;
- detection latency після входу в Shorts менше 1 секунди в більшості тестів;
- всі debug logs вимкнені в release build.
```

---

## Phase 7 — Blocking Layer

**Статус зараз:** DONE  
**Мета:** зробити блокування простим, зрозумілим і стабільним.

### Рекомендований механізм

Основний варіант:

```text
TYPE_ACCESSIBILITY_OVERLAY
```

Чому:

```text
- overlay належить AccessibilityService;
- не потрібно окремо просити SYSTEM_ALERT_WINDOW;
- менше зайвих дозволів;
- краще відповідає задачі: показати блокуючий екран саме після detection.
```

### Поведінка

Коли Shorts визначено:

```text
1. Показати full-screen accessibility overlay.
2. На overlay показати:
   - “Shorts заблоковано”
   - “Потрібен PIN батьків”
   - кнопка “Вийти з Shorts”
   - кнопка “Ввести PIN”
3. Якщо натиснути “Вийти з Shorts”:
   - performGlobalAction(GLOBAL_ACTION_HOME)
   - залишити кнопку disabled на короткий час
   - автоматично закрити overlay після успішного HOME action
4. Якщо ввести PIN:
   - показати temporary allow options
```

### Важливо

Не робити так, щоб overlay виглядав як системне попередження Android. Він має бути явно брендовим екраном Shorts Blocker Kids.

### Не входить у фазу

```text
- прихований overlay;
- обманні системні діалоги;
- блокування налаштувань Android;
- блокування видалення додатку;
- будь-яке залякування дитини.
```

### Done

Фаза завершена, якщо:

```text
- overlay зʼявляється при Shorts;
- overlay не зʼявляється в інших додатках;
- overlay не зʼявляється на звичайному YouTube-відео;
- кнопка “Вийти з Shorts” повертає з Shorts;
- кнопка “Вийти з Shorts” не клікає YouTube UI, не шукає YouTube Home tab і не відкриває YouTube URL;
- PIN відкриває temporary allow;
- overlay не зависає після виходу з YouTube;
- overlay прибирається при вимкненні protection;
- overlay прибирається при temporary allow;
- UI нормально працює в portrait і landscape;
- UI читається на малому екрані.
```

---

## Phase 8 — Temporary Allow + Parent Control Flow

**Статус зараз:** DONE  
**Мета:** дати батькам контроль без складності.

### Сценарії

#### Сценарій 1 — Батько дозволяє на 5 хвилин

```text
Дитина відкрила Shorts
→ overlay
→ батько вводить PIN
→ обирає 5 хвилин
→ overlay закривається
→ Shorts доступний 5 хвилин
→ після 5 хвилин блокування знову активне
```

#### Сценарій 2 — Батько вимикає захист

```text
Батько відкриває dashboard
→ вводить PIN
→ вимикає Protection
→ додаток показує статус Protection OFF
```

#### Сценарій 3 — Дитина не знає PIN

```text
Дитина відкрила Shorts
→ overlay
→ PIN не введено
→ Shorts не доступний
```

### Local state

```text
temporaryAllowUntil:
- null, якщо дозволу немає;
- timestamp, якщо дозвіл активний;
- автоматичне очищення після expiry.
```

### Done

Фаза завершена, якщо:

```text
- temporary allow працює для 5/10/15 хвилин;
- після завершення часу Shorts знову блокується;
- temporary allow переживає перезапуск додатку;
- expired allow не працює після зміни системного часу назад у простому тесті;
- вимкнення protection потребує PIN;
- зміна PIN потребує старий PIN або recovery-flow.
```

### Recovery-flow

На першому релізі можна зробити простий recovery:

```text
Якщо PIN забуто:
- видалити додаток і налаштувати заново.
```

Не робити email recovery, бо немає акаунта і сервера.

---

## Phase 9 — Honest Anti-Bypass Guardrails

**Статус зараз:** DONE  
**Мета:** не створювати фальшиву обіцянку “неможливо обійти”, але зробити базовий захист від випадкового вимкнення.

### Що можна робити

```text
- PIN для dashboard;
- PIN для вимкнення protection;
- чіткий статус “Accessibility Service вимкнено”;
- екран “Захист неактивний, увімкніть дозвіл”;
- локальна перевірка стану сервісу при відкритті додатку;
- спокійна інструкція для батьків;
- optional persistent notification “Shorts protection is active”, якщо це допомагає прозорості.
```

### Що не робити у v1

```text
- не приховувати додаток;
- не маскуватися під системний сервіс;
- не забороняти видалення додатку обхідними методами;
- не блокувати Android Settings агресивно;
- не робити deceptive UI;
- не перехоплювати всі екрани;
- не ставити Device Owner як обовʼязкову умову.
```

### Чесний текст у додатку

```text
Shorts Blocker Kids працює, коли захист увімкнено і Android Accessibility Service активний.
Якщо системний дозвіл вимкнути або додаток видалити, блокування перестане працювати.
```

### Done

Фаза завершена, якщо:

```text
- додаток не обіцяє абсолютний захист;
- батьки бачать статус дозволу;
- без Accessibility Service dashboard показує “Protection inactive”;
- немає прихованих anti-uninstall механізмів;
- немає policy-ризикових дій без окремого рішення.
```

---

## Phase 10 — Google Play Billing Monetization

**Статус зараз:** Partial
**Мета:** довести monetization до Google Play production-ready стану через Google Play Billing.

### Active launch channel

```text
Active production path:
- Google Play Store;
- Android App Bundle;
- Google Play Billing;
- Play-managed subscription purchase flow;
- app entitlement state from purchase state.

Deferred / frozen:
- website APK paid launch;
- Stripe Checkout;
- Stripe subscription backend;
- manual license key;
- code entry activation.
```

### Бізнес-модель

```text
Платно:
- €2.20 / місяць
- recurring monthly subscription

Payment provider for Play Store launch:
- Google Play Billing

Subscription management:
- Google Play subscription management screen
```

### Важливе рішення

Якщо додаток продає digital app functionality через Google Play, треба використовувати Google Play Billing.

Website APK + Stripe не виконувати, поки Play Store production-track не доведено до кінця.

Backend purchase verification бажаний для production-рівня, але не є першим implementation-кроком, поки не завершено policy package і real-device QA.

Backend, якщо буде доданий пізніше, не повинен отримувати child data, YouTube data, browsing history, video data або app usage details.

Backend може отримувати тільки billing-технічні дані:

```text
- install identifier;
- subscription status;
- current period end;
- Play purchase token / subscription product ID;
- activation status.
```

### Subscription states

```text
UNKNOWN
ACTIVE
CANCELED_BUT_ACTIVE_UNTIL_END
PAST_DUE
UNPAID
EXPIRED
OFFLINE_GRACE
```

### Поведінка

```text
ACTIVE:
- protection може працювати.

CANCELED_BUT_ACTIVE_UNTIL_END:
- protection працює до current_period_end.

OFFLINE_GRACE:
- protection може працювати 72 години після останньої успішної active entitlement перевірки;
- grace не може тривати довше ніж current_period_end + 24 години.

PAST_DUE або UNPAID:
- dashboard показує payment problem;
- protection блокується після завершення дозволеного grace.

EXPIRED:
- dashboard показує subscription expired;
- Shorts blocking недоступний;
- батько все одно може зайти в налаштування через PIN;
- не можна блокувати доступ до вимкнення сервісу.
```

### Не входить у Play Store monetization track

```text
- manual license key;
- code entry activation;
- app account system;
- child profiles;
- cloud rules;
- sync;
- analytics;
- child/YouTube activity upload;
- Stripe launch path.
```

### Phase 10A — Billing Policy Lock

**Статус зараз:** TODO

Done:

```text
- Google Play Billing is confirmed as active Play Store monetization path;
- Stripe / website APK billing is explicitly deferred;
- no alternate payment links are shown inside Play-distributed app;
- subscription product IDs are planned;
- entitlement rules are documented.
```

### Phase 10B — Play Billing App Integration

**Статус зараз:** TODO

Done:

```text
- Play Billing dependency is added;
- BillingClient lifecycle is implemented;
- product details loading works;
- purchase flow starts from parent-only screen;
- pending/canceled/error states are handled;
- purchase acknowledgement is implemented;
- restore purchases works;
- Manage subscription link opens Google Play subscription management.
```

### Phase 10C — Entitlement Integration

**Статус зараз:** TODO

Done:

```text
- active subscription enables protection;
- expired subscription blocks protection feature but never blocks access to settings;
- canceled-but-active subscription remains active until period end;
- payment problem state is visible to parent;
- local cached entitlement has conservative expiry;
- free test / paid entitlement precedence is documented and tested.
```

### Phase 10D — Optional Backend Verification

**Статус зараз:** Deferred

Done:

```text
- backend requirement is decided before production rollout;
- if implemented, purchase verification happens on secure backend;
- backend stores only billing technical data;
- real-time subscription lifecycle handling is documented;
- no child/YouTube activity is uploaded.
```

### Phase 10E — Billing QA

**Статус зараз:** TODO

Done:

```text
- license tester purchase works;
- subscription renewal/cancel/expire paths are tested;
- pending purchase is tested if available;
- restore purchases is tested after reinstall;
- Manage subscription path is tested;
- expired/unpaid state locks protection;
- app never directs Play users to non-Play payment.
```

### Google Play policy note

Якщо колись буде окремий website APK distribution, його треба планувати як окремий track після Play Store production і не змішувати з Play-distributed billing UX.

---

## Phase 11 — Privacy, Policy, Google Play Compliance

**Статус зараз:** Partial / P0
**Мета:** підготувати додаток так, щоб Google Play review не виглядав як лотерея.

Поточний факт:

```text
- repo-side Play policy package prepared;
- Accessibility declaration draft prepared;
- Data Safety draft prepared for the current local-only build;
- Privacy Policy draft prepared;
- store listing draft prepared;
- reviewer instructions prepared;
- external Play Console submission, hosted Privacy Policy URL, privacy contact,
  demo video URL, screenshots, target audience, and content rating remain pending.
```

### AccessibilityService policy checklist

Потрібно підготувати:

```text
- prominent disclosure всередині додатку;
- affirmative consent: чекбокс або окрема кнопка;
- чітке пояснення, які дані доступні через AccessibilityService;
- чітке пояснення, для чого це використовується;
- чітке пояснення, що не збирається і не передається;
- Play Console Accessibility declaration;
- short review video;
- app description, яка чесно описує Accessibility use.
```

### Review demo video

Відео для Play review має показати:

```text
1. Відкриття додатку.
2. Prominent Accessibility disclosure і повний текст disclosure.
3. Consent flow: користувач погоджується і переходить до Android Accessibility settings.
4. Grant permission flow: увімкнення Shorts Blocker Kids service.
5. Decline/no-consent flow або повторний показ disclosure перед permission.
6. Core feature: YouTube Shorts відкрито → blocker overlay показано.
7. Exit to phone home: натиснуто кнопку → Android HOME action → overlay auto-dismiss.
8. Parent PIN / temporary allow flow.
```

Якщо поведінка AccessibilityService не очевидна з UI, додати voice-over або captions.

### Не ставити isAccessibilityTool=true

Shorts Blocker Kids не є інструментом для людей з інвалідністю. Тому не треба позначати його як accessibility tool. Правильна позиція:

```text
Parental digital wellbeing / screen-control tool.
AccessibilityService використовується для локального визначення YouTube Shorts і показу блокуючого екрана.
```

### Privacy Policy

Privacy Policy має прямо сказати:

```text
- додаток працює локально;
- немає власного сервера;
- немає акаунта;
- правила зберігаються на пристрої;
- PIN зберігається як hash + salt;
- додаток не зберігає історію переглядів;
- додаток не записує екран;
- додаток не записує звук;
- додаток не читає приватні повідомлення;
- додаток не продає дані;
- Google Play Billing може обробляти платіжні дані відповідно до правил Google.
```

### Data Safety

Якщо в релізі немає analytics і немає власного backend:

```text
Data collection:
- власний додаток: no collection / no sharing для YouTube activity;
- Play Billing: розкрити згідно з Google Play requirements і SDK behavior.
```

### Store listing

Опис має бути простий:

```text
Shorts Blocker Kids helps parents block YouTube Shorts on a child’s Android phone.
No account. No app server. Parent PIN and local rules.
```

### App Access для review

Для Google Play reviewer:

```text
- надати тестовий PIN;
- пояснити, як увімкнути AccessibilityService;
- пояснити, як протестувати YouTube Shorts blocking;
- надати demo video;
- надати тестову інструкцію для subscription.
```

### Done

Фаза завершена, якщо:

```text
- є Privacy Policy URL;
- Privacy Policy доступна в додатку;
- Data Safety form заповнена;
- Accessibility declaration заповнена;
- review video записано;
- app description не вводить в оману;
- disclosure видно до ввімкнення AccessibilityService;
- consent є окремою дією;
- release build не містить debug logs accessibility-tree;
- release build не містить зайвих дозволів.
```

---

## Phase 12 — Testing Matrix

**Статус зараз:** Partial
**Мета:** довести, що простий додаток працює стабільно на реальних пристроях.

Поточний факт:

```text
User-confirmed real-device smoke QA passed on five devices:
- app installs;
- PIN works;
- Accessibility Service can be enabled;
- normal YouTube videos are not blocked;
- YouTube Shorts are blocked immediately;
- exit to phone home works;
- PIN temporary allow works for 5, 10, and 15 minutes.

Detailed device model / Android version / YouTube version matrix is still pending.
```

### Мінімальний набір пристроїв

```text
- Pixel / чистий Android
- Samsung
- Xiaomi / Redmi
- OnePlus або Motorola
- старіший Android-пристрій
- новіший Android-пристрій
```

### Android versions

```text
- Android 10
- Android 11
- Android 12
- Android 13
- Android 14
- Android 15
- актуальна версія, доступна для тестування на момент релізу
```

### YouTube scenarios

```text
1. YouTube home → Shorts tab
2. YouTube home → Shorts card
3. Search → Shorts result
4. Channel page → Shorts
5. Direct Shorts link
6. Swipe between Shorts
7. Return from Shorts
8. Open normal video
9. Open playlist
10. Open search
11. Open comments on normal video
12. Open YouTube app after reboot
```

### Expected result

```text
Shorts:
- block.

Normal YouTube:
- do not block.

Other apps:
- do not block.
```

### Performance checks

```text
- detection under 1 second in normal case;
- no repeated flickering overlay;
- no infinite back loop;
- no ANR;
- no crash when YouTube updates UI tree;
- no crash when root node is null;
- no crash when AccessibilityService is disabled mid-session.
```

### Privacy checks

```text
- no network calls except Google Play Billing if billing is enabled;
- no accessibility-tree logs in release;
- no video titles stored;
- no URLs stored;
- no screen recording;
- no microphone permission;
- no camera permission;
- no location permission.
```

### Done

Фаза завершена, якщо:

```text
- test matrix пройдена;
- є таблиця результатів по пристроях;
- є список known issues;
- критичні помилки виправлені;
- false positives на звичайному YouTube нижче 1%;
- blocking works у більшості реальних Shorts-сценаріїв;
- додаток не падає при вимкненні AccessibilityService;
- release candidate готовий до closed testing.
```

---

## Phase 13 — Closed Testing / Beta

**Статус зараз:** TODO  
**Мета:** перевірити додаток не тільки на своїх пристроях.

### Тестери

Мінімум:

```text
- 12+ тестерів, якщо це потрібно для вашого типу Google Play developer account;
- бажано 20–30 реальних батьків або користувачів Android;
- різні телефони;
- різні версії YouTube;
- різні країни/мови інтерфейсу.
```

### Що просити тестувати

```text
1. Чи зрозуміло, що робить додаток?
2. Чи легко створити PIN?
3. Чи зрозуміло, як увімкнути AccessibilityService?
4. Чи блокується Shorts?
5. Чи не блокується звичайний YouTube?
6. Чи не зависає overlay?
7. Чи дитина може випадково вимкнути захист?
8. Чи не страшний текст для дитини?
9. Чи готові батьки платити €2.20 / місяць?
10. Що саме незрозуміло в onboarding?
```

### Метрики beta

```text
- onboarding completion rate;
- accessibility enabled rate;
- first successful block rate;
- false positive reports;
- crash-free sessions;
- subscription screen conversion intent;
- user confusion points.
```

Без analytics у першому релізі ці метрики можна збирати вручну через форму feedback.

### Done

Фаза завершена, якщо:

```text
- мінімум 12–20 людей протестували додаток;
- є feedback document;
- знайдені top 10 проблем;
- критичні проблеми виправлені;
- onboarding став зрозуміліший;
- detection працює на більшості пристроїв;
- release candidate пройшов повторний smoke test.
```

---

## Phase 14 — Public Release v1

**Статус зараз:** TODO  
**Мета:** випустити простий, чесний, якісний перший реліз.

### Release package

```text
- AAB build;
- signed release;
- versionName 1.0.0;
- versionCode 1;
- targetSdk актуальний для Google Play;
- minSdk фінально підтверджений;
- no debug logs;
- no debug snapshot tools;
- no unused permissions.
```

### Store assets

```text
- app icon;
- feature graphic;
- screenshots:
  1. головна обіцянка;
  2. PIN;
  3. protection ON;
  4. Shorts blocked;
  5. no account / no server / local rules;
- short description;
- full description;
- privacy policy;
- support email;
- accessibility review video.
```

### Release criteria

```text
Не випускати, якщо:
- є crash при відкритті YouTube;
- є масові false positives на звичайному YouTube;
- PIN зберігається plain text;
- немає prominent disclosure;
- немає Privacy Policy;
- немає Data Safety;
- немає Accessibility declaration;
- overlay можна зациклити;
- subscription ламає доступ до налаштувань;
- app misleading у Play listing.
```

### Done

Фаза завершена, якщо:

```text
- додаток прийнятий у Google Play;
- перший public build доступний;
- support email працює;
- є план швидкого hotfix;
- є список known limitations;
- є process для YouTube UI regression fixes.
```

---

## Phase 15 — Maintenance Loop

**Статус зараз:** TODO  
**Мета:** підтримувати стабільність, бо YouTube може змінювати UI.

### Щотижневий контроль

```text
- перевірити latest YouTube;
- перевірити Shorts detection;
- перевірити normal video false positives;
- перевірити Play Billing entitlement;
- переглянути support reports;
- виправити detector rules, якщо YouTube змінив IDs/layout.
```

### Detector updates

```text
- не ламати старі правила;
- додавати нові сигнали;
- мати regression checklist;
- не включати debug snapshots у release;
- кожне правило має мати reason.
```

### Done

Цикл працює, якщо:

```text
- є регулярний smoke test;
- issues по false positives не накопичуються;
- detector оновлюється швидко;
- Google Play policy не порушується;
- продукт залишається простим.
```

---

# Implementation Details

## A. Рекомендований порядок задач для розробника

```text
Current implementation priority:

1. Freeze scope under Active Production Execution Lock.
2. Finish Accessibility disclosure / consent UX.
3. Prepare Play Accessibility declaration answers.
4. Record review demo video script and final video.
5. Finalize Privacy Policy and in-app privacy link.
6. Finalize Data Safety answers.
7. Decide target audience before store listing.
8. Run real-device blocker QA matrix.
9. Build AAB release pipeline and signing/upload-key flow.
10. Add Google Play Billing.
11. Integrate billing entitlement into protection state.
12. Add subscription management / restore flow.
13. Run billing QA with license testers.
14. Prepare store listing assets and reviewer notes.
15. Upload AAB to internal testing.
16. Run closed testing 14+ days if account requires it.
17. Apply for production access if required.
18. Run staged production rollout.
19. Maintain hotfix loop for YouTube UI changes.
```

---

## B. Мінімальні permissions

Очікувано потрібні:

```text
android.permission.BIND_ACCESSIBILITY_SERVICE
```

Можливо потрібні залежно від рішення:

```text
POST_NOTIFICATIONS
```

Не брати без крайньої потреби:

```text
SYSTEM_ALERT_WINDOW
INTERNET
CAMERA
MICROPHONE
LOCATION
READ_CONTACTS
READ_SMS
QUERY_ALL_PACKAGES
```

Коментар:

```text
INTERNET може не бути потрібним, якщо немає власного backend і немає analytics.
Google Play Billing працює через Play infrastructure, але це треба перевірити в конкретній реалізації.
```

---

## C. Release build rules

```text
Release build must:
- disable debug tree snapshot;
- disable raw AccessibilityNodeInfo logging;
- disable verbose detector logs;
- remove test PINs;
- remove debug buttons;
- not include fake billing states;
- not include test-only UI;
- not contain hardcoded personal data;
- not request unused permissions.
```

---

## D. Detector rule design

Правило не має бути просто:

```text
if text contains "Shorts" -> block
```

Правильніше:

```text
if package == YouTube
and protectionEnabled == true
and temporaryAllowExpired == true
and detector confidence == HIGH
then block
```

Приклад reasons:

```text
- youtube_package
- shorts_container_id
- reel_recycler_present
- fullscreen_vertical_player
- shorts_controls_present
```

---

## E. UI tone

Тон додатку:

```text
- спокійний;
- сімейний;
- короткий;
- без залякування;
- без сорому для дитини;
- без агресії;
- без політики;
- без зайвої моралі.
```

Правильний текст:

```text
Shorts заблоковано.
Потрібен PIN батьків.
```

Поганий текст:

```text
Ти знову витрачаєш час.
Ти не повинен це дивитися.
Це небезпечно.
```

---

## F. Головні ризики

### Risk 1 — Google Play Accessibility review

Ризик:

```text
Google Play може відхилити додаток, якщо disclosure слабкий або use-case виглядає як прихований контроль.
```

Рішення:

```text
- чесний disclosure;
- окрема згода;
- privacy policy;
- review video;
- не ставити isAccessibilityTool=true;
- не приховувати функцію;
- описувати як parental digital wellbeing tool.
```

### Risk 2 — YouTube UI changes

Ризик:

```text
YouTube може змінити IDs/layout.
```

Рішення:

```text
- rule-engine;
- кілька сигналів;
- regression tests;
- швидкі оновлення;
- support channel для reports.
```

### Risk 3 — False positives

Ризик:

```text
Додаток може блокувати звичайний YouTube.
```

Рішення:

```text
- блокувати тільки HIGH confidence;
- не блокувати тільки через текст;
- тестувати normal video scenarios;
- додати cooldown і state machine.
```

### Risk 4 — Android limitations

Ризик:

```text
Користувач з доступом до системних налаштувань може вимкнути сервіс або видалити додаток.
```

Рішення:

```text
- чесно пояснити обмеження;
- показувати статус захисту;
- не обіцяти неможливий захист;
- не будувати risky anti-uninstall у v1.
```

### Risk 5 — Subscription without backend

Ризик:

```text
Локальна перевірка entitlement менш захищена від обходу, ніж secure backend verification.
```

Рішення:

```text
- використовувати Google Play Billing для Play-distributed app;
- acknowledge purchases;
- мати conservative cached entitlement;
- до production rollout окремо вирішити, чи потрібен backend verification;
- якщо backend додається, не передавати child/YouTube activity data.
```

---

# v1 Release Definition

Перший реліз можна вважати готовим, якщо:

```text
1. Батько встановлює додаток.
2. Створює PIN.
3. Приймає чесний Accessibility disclosure.
4. Вмикає AccessibilityService.
5. Вмикає YouTube Shorts Protection.
6. Дитина відкриває YouTube Shorts.
7. Додаток блокує Shorts.
8. Звичайний YouTube не блокується.
9. Вимкнути захист можна тільки через PIN.
10. Тимчасовий доступ працює.
11. Немає акаунта.
12. Немає власного сервера.
13. Немає історії переглядів.
14. Немає аналізу контенту.
15. Немає зайвих дозволів.
16. Google Play policy checklist виконаний.
```

---

# Що залишити на майбутнє, але не робити в v1

```text
- Block YouTube повністю;
- schedule;
- learning break;
- math challenge;
- parent notification;
- multi-device;
- web dashboard;
- child profiles;
- remote config;
- server;
- analytics;
- app usage statistics;
- blocking Reels/TikTok;
- device-owner strong mode.
```

Це можна додавати тільки після того, як головна функція працює стабільно:

```text
YouTube Shorts відкрився → Shorts заблоковано.
```

---

# Джерела, перевірені для roadmap

Ці джерела використані для технічних і policy-рішень. Код з GitHub-репозиторіїв не потрібно копіювати напряму; їх варто використовувати як технічні приклади й реалізувати власне чисте рішення.

1. Google Play — Use of the AccessibilityService API  
   https://support.google.com/googleplay/android-developer/answer/10964491

2. Android Developers — AccessibilityService API reference  
   https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

3. Android Developers — AccessibilityServiceInfo API reference  
   https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo

4. Android Developers — WindowManager.LayoutParams / TYPE_ACCESSIBILITY_OVERLAY  
   https://developer.android.com/reference/android/view/WindowManager.LayoutParams

5. Android Developers — Target API level requirement  
   https://developer.android.com/google/play/requirements/target-sdk

6. Google Play — Payments policy / Play Billing requirement  
   https://support.google.com/googleplay/android-developer/answer/10281818

7. Android Developers — Play Billing subscriptions  
   https://developer.android.com/google/play/billing/subscriptions

8. Android Developers — Jetpack DataStore  
   https://developer.android.com/jetpack/androidx/releases/datastore

9. Google Play — User Data policy  
   https://support.google.com/googleplay/android-developer/answer/10144311

10. Google Play — Data Safety section  
    https://support.google.com/googleplay/android-developer/answer/10787469

11. GitHub reference — yadavnikhil03/AntiScroll  
    https://github.com/yadavnikhil03/AntiScroll

12. GitHub reference — atick-faisal/Shorts-Blocker  
    https://github.com/atick-faisal/Shorts-Blocker

13. Google Play — App testing requirements for new personal developer accounts  
    https://support.google.com/googleplay/android-developer/answer/14151465

14. Android Developers — Android App Bundle
    https://developer.android.com/guide/app-bundle

---

# Фінальний принцип

Shorts Blocker Kids має бути простим ззовні і сильним всередині.

```text
Для батьків:
один перемикач, один PIN, один результат.

Для розробки:
чистий Android native code, чесний Accessibility flow, стабільний detector, мінімум permissions, сильний testing, повна policy-підготовка.
```

Головний Done всього продукту:

```text
Батько встановив додаток, ввів PIN, увімкнув захист — і YouTube Shorts більше не відкривається без його дозволу.
```
