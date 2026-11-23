# Как запрос проходит через приложение

Разберём путь от кнопки или клавиши до обновления поля. Ссылки открывают файлы реализации; нужные методы показаны во фрагментах кода.

Контракт проходит цепочку **Java API → runtime OpenAPI → docs/openapi.yaml → TypeScript SDK → React**. Команда `make api-generate` экспортирует схему из временного бэка и генерирует методы и модели через [orval.config.ts](../frontend/orval.config.ts). Генератор работает при разработке и сборочных проверках, а в браузере выполняется уже готовый SDK.

React импортирует операции из [api.gen.ts](../frontend/src/api/generated/api.gen.ts), а модели — из [generated/models](../frontend/src/api/generated/models/index.ts). URL, HTTP-методы и сериализация тела находятся в сгенерированном коде. Общий [http.ts](../frontend/src/api/http.ts) выполняет fetch с таймаутом 10 секунд и передаёт ошибки; игровых маршрутов и моделей в нём нет.

Браузер открывает React на порту 5173. SDK отправляет относительные запросы `/api/...`; локальный Vite перенаправляет их на бэк (`http://127.0.0.1:8080` по умолчанию, изменяется через `API_URL`). [Swagger UI](http://127.0.0.1:8080/swagger-ui.html) — отдельный клиент бэка для ручных HTTP-запросов. Он и генератор SDK используют один контракт. `make check` сверяет runtime с YAML и YAML с SDK без перезаписи файлов.

OpenAPI бэка сейчас ограничен маршрутами `/api/games/**`, а описание ответов дополняет `OpenApiConfiguration`. Для нового контроллера вне этой области сначала расширяют документирование на бэке. После появления операции в YAML генератор создаёт её метод и модели; React подключает их к нужному экрану.

## 1. Пользователь нажимает «Новая игра»

React вызывает `createGame()` и сохраняет результат:

```typescript
setGame(await createGame());
```

Функция сгенерирована Orval в [api.gen.ts](../frontend/src/api/generated/api.gen.ts) по operationId `createGame`:

```typescript
export const createGame = async (
  options?: Parameters<typeof request>[1],
): Promise<GameSnapshot> => {
  return request<GameSnapshot>(getCreateGameUrl(), {
    ...options,
    method: 'POST',
  });
};
```

Браузер отправляет `POST /api/games`. Spring передаёт запрос методу `create()` в [GameController](../backend/api/src/main/java/dev/tetris/api/GameController.java):

```java
public ResponseEntity<GameSnapshot> create() {
    GameSnapshot snapshot = games.create().snapshot();
    return ResponseEntity.created(URI.create("/api/games/" + snapshot.gameId())).body(snapshot);
}
```

Здесь `games` — внедрённый `GameRegistry`. Контроллер просит создать партию, получает её снимок и возвращает **201 Created**. Заголовок `Location` содержит путь чтения созданной партии.

Внутри [GameRegistry.create()](../backend/api/src/main/java/dev/tetris/api/GameRegistry.java):

```java
public TetrisGame create() {
    TetrisGame game;
    try {
        game = factory.get();
    } catch (RuntimeException exception) {
        throw new GameOperationException("game_creation_failed", null, null, exception);
    }
    var state = game.snapshot();
    games.put(state.gameId(), game);
    LOGGER.info("event=game_created gameId={} width={} height={}",
            state.gameId(), state.width(), state.height());
    return game;
}
```

- `factory.get()` создаёт отдельный экземпляр `TetrisGame`.
- `games.put(...)` сохраняет партию в памяти по её UUID.
- `return game` возвращает объект контроллеру.

Для обычного запуска фабрика настроена в [GameConfiguration](../backend/api/src/main/java/dev/tetris/api/GameConfiguration.java):

```java
public Supplier<TetrisGame> gameFactory() {
    return TetrisGame::new;
}
```

Поэтому `factory.get()` вызывает `new TetrisGame()`. Конструктор создаёт поле **10 × 20**, источник семи стандартных типов и выпускает первую фигуру. React сохраняет полученный снимок через `setGame()` и отображает поле.

## 2. Пользователь нажимает «Влево» или стрелку ←

Кнопка и клавиша используют общий `run()`: обработчик клавиатуры в [useKeyboardControls.ts](../frontend/src/useKeyboardControls.ts) переводит стрелки в команды, Пробел — в `HARD_DROP`, а P / Esc — в локальную паузу. Редактируемые элементы, сочетания с модификаторами и IME сохраняют поведение браузера. После успешной новой игры фокус переходит на поле.

Экран передаёт UUID и команду в сгенерированный [executeCommand()](../frontend/src/api/generated/api.gen.ts):

```typescript
export const executeCommand = async (
  id: string,
  commandBody: CommandBody,
  options?: Parameters<typeof request>[1],
): Promise<CommandOutcome> => {
  return request<CommandOutcome>(getExecuteCommandUrl(id), {
    ...options,
    method: 'POST',
    headers: getHeaders({ 'Content-Type': 'application/json' }, options?.headers),
    body: JSON.stringify(commandBody),
  });
};
```

Путь строит сгенерированный `getExecuteCommandUrl(id)`. Общий транспорт получает уже подготовленные URL и options.

Запрос содержит UUID текущей партии и команду:

```http
POST /api/games/{id}/commands
Content-Type: application/json

{"command": "LEFT"}
```

В [GameController.command()](../backend/api/src/main/java/dev/tetris/api/GameController.java) проверяется обязательность команды, затем выполняется основной вызов:

```java
public CommandOutcome command(@PathVariable UUID id, @RequestBody CommandBody body) {
    if (body.command() == null) {
        LOGGER.warn("event=command_rejected gameId={} reason=missing_command", id);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите команду");
    }
    TetrisGame game = games.get(id);
    CommandOutcome outcome;
    try {
        outcome = game.execute(body.command());
    } catch (RuntimeException exception) {
        throw new GameOperationException("game_command_failed", id, body.command(), exception);
    }
    LOGGER.debug("event=game_command gameId={} command={} result={} clearedLines={}",
            id, body.command(), outcome.result(), outcome.state().clearedLines());
    if (outcome.result() == CommandResult.APPLIED && outcome.state().status() == GameStatus.GAME_OVER) {
        LOGGER.info("event=game_finished gameId={} clearedLines={}", id, outcome.state().clearedLines());
    }
    return outcome;
}
```

Вызовы `games.get(id)` и `game.execute(body.command())` означают: **найти партию по UUID → выполнить у неё команду**. Результат записывается на DEBUG, первое окончание — на INFO. При неожиданном сбое `GameOperationException` сохраняет UUID, команду и исходную причину. [ApiErrors](../backend/api/src/main/java/dev/tetris/api/ApiErrors.java) записывает ERROR один раз со стеком и возвращает HTTP 500 в формате `problem+json`, без внутренних деталей исключения.

Поиск находится в [GameRegistry.get()](../backend/api/src/main/java/dev/tetris/api/GameRegistry.java):

```java
public TetrisGame get(UUID id) {
    TetrisGame game = games.get(id);
    if (game == null) {
        LOGGER.warn("event=game_missing gameId={}", id);
        throw new GameNotFoundException();
    }
    return game;
}
```

Если партии нет, [ApiErrors](../backend/api/src/main/java/dev/tetris/api/ApiErrors.java) преобразует исключение в HTTP **404**.

Подпись на схеме «создать партию / найти по ID» обозначает отдельные операции:

| Запрос | Метод контроллера | Обращение к реестру |
|---|---|---|
| `POST /api/games` | `create()` | `games.create()` — новая партия |
| `GET /api/games/{id}` | `read(id)` | `games.get(id)` — поиск и чтение снимка |
| `POST /api/games/{id}/commands` | `command(id, body)` | `games.get(id)` — поиск для выполнения команды |

Поиск отсутствующей партии возвращает ошибку; новая партия создаётся только отдельным запросом `POST /api/games`.

## 3. Ядро проверяет перемещение или поворот

[TetrisGame.execute()](../backend/core/src/main/java/dev/tetris/core/TetrisGame.java) создаёт кандидат размещения:

```java
ActivePiece candidate = switch (command) {
    case LEFT -> active.translated(-1, 0);
    case RIGHT -> active.translated(1, 0);
    case DOWN -> active.translated(0, 1);
    case ROTATE -> active.rotated();
    case HARD_DROP -> landingPosition();
};
```

Для сдвига и поворота `ActivePiece` создаёт новое размещение. Для `HARD_DROP` метод `landingPosition()` проверяет последовательные позиции через поле и возвращает самую низкую допустимую. Эти вычисления сохраняют текущую фигуру без изменений.

[Board.canPlace()](../backend/core/src/main/java/dev/tetris/core/Board.java) проверяет границы и занятость каждой клетки:

```java
boolean canPlace(List<Cell> cells) {
    return cells.stream().allMatch(cell -> cell.x() >= 0 && cell.x() < width
            && cell.y() >= 0 && cell.y() < height && !occupied[cell.y()][cell.x()]);
}
```

Для обычного сдвига или поворота допустимое размещение становится активным через `active = candidate`. Заблокированный боковой сдвиг или поворот возвращает `BLOCKED`, сохраняя состояние. `HARD_DROP` сразу передаёт найденную посадку на закрепление; заблокированный `DOWN` закрепляет текущее размещение.

Для поворота [ActivePiece.rotated()](../backend/core/src/main/java/dev/tetris/core/ActivePiece.java) обращается к определению типа:

```java
ActivePiece rotated() {
    return new ActivePiece(type, type.rotate(localCells), x, y);
}
```

Именно `type.rotate(...)` обеспечивает полиморфизм: особенности вращения находятся внутри `Tetromino` и его наследников. Например, `O` сохраняет клетки на месте. Партия применяет общий алгоритм проверки любого результата.

## 4. Фигура достигает дна или других клеток

Автоматическое падение использует ту же команду `DOWN`. [Таймер React](../frontend/src/App.tsx) отправляет её через выбранный интервал:

```typescript
const timer = window.setInterval(() => void run('DOWN'), fallInterval);
return () => window.clearInterval(timer);
```

Пока выполняется запрос, новые тики пропускаются. При локальной паузе, ошибке отправленной команды или окончании партии падение останавливается. Кнопка «Пауза / Продолжить» переключает состояние текущей вкладки без HTTP-запроса: таймер удаляется, игровые кнопки отключены. Продолжение создаёт новый таймер с полным выбранным интервалом. Успешное создание новой партии снимает паузу.

Если шаг вниз невозможен, [TetrisGame](../backend/core/src/main/java/dev/tetris/core/TetrisGame.java) вызывает общий метод `lockAndSpawn(piece)` со следующей последовательностью:

```java
ActivePiece next = ActivePiece.spawn(source.next(), width);
clearedLines += board.lock(piece.cells());
spawn(next);
```

1. Получает следующую фигуру и подготавливает её стартовую позицию. При ошибке источника состояние партии сохраняется.
2. `Board.lock()` закрепляет текущие клетки, удаляет заполненные строки и опускает оставшиеся. Возвращённое число удалённых строк прибавляется к счётчику.
3. `spawn(next)` проверяет подготовленную фигуру на обновлённом поле. Если места нет, устанавливает `GAME_OVER` и убирает активную фигуру.

**Мгновенный сброс** отправляет один запрос с `{ "command": "HARD_DROP" }`. Метод `landingPosition()` находит самое низкое размещение через ту же проверку `Board.canPlace`. Затем партия передаёт найденную посадку в общий `lockAndSpawn()` и возвращает результат уже после закрепления, очистки и выпуска следующей фигуры. Промежуточные шаги вниз не отправляются по HTTP и не меняют состояние партии. Если источник следующей фигуры недоступен, исходная позиция остаётся прежней.

## 5. Результат возвращается на экран

В конце обработки `TetrisGame` возвращает результат и снимок:

```java
return new CommandOutcome(result, snapshot());
```

Методы `execute()` и `snapshot()` синхронизированы на экземпляре партии. Снимок результата формируется в рамках той же операции и содержит неизменяемые списки клеток.

Контроллер возвращает `CommandOutcome`, Spring преобразует его в JSON, а [React обновляет состояние](../frontend/src/App.tsx):

```typescript
const outcome = await executeCommand(gameId, { command: action as Command });
setGame(outcome.state);
```

После `setGame()` экран отображает активную фигуру, закреплённые клетки, счётчик строк и статус. Игровое поле — интерактивная сетка `grid` с рядами `row` и клетками `gridcell`. После успешного создания партии фокус переводится на поле; пользователь может переключить его на кнопки или список скорости.

После ошибки команды экран сохраняет последний подтверждённый снимок, останавливает таймер и блокирует игровые действия. Кнопка «Обновить состояние» отправляет `GET /api/games/{id}` без повторного выполнения команды. После успешного чтения управление возобновляется, если партия остаётся `RUNNING` и локальная пауза выключена. При `GAME_OVER` остаётся доступна новая игра.

Ошибка создания новой партии сохраняет прежнюю партию и её состояние паузы. При создании новой партии сброс паузы и замена UUID происходят только после успешного ответа.

По умолчанию журнал API работает на INFO: видны создание и окончание партии. Отдельные команды выводятся при включении DEBUG. Локальные переключения паузы запросов не отправляют и в серверный журнал не попадают.

Весь путь одной команды:

```text
Кнопка React, клавиатура или таймер
  → сгенерированный executeCommand()
  → общий транспорт request()
  → HTTP-запрос
  → GameController.command()
  → GameRegistry.get(id)
  → TetrisGame.execute(command)
  → кандидат ActivePiece и проверка Board
  → CommandOutcome со снимком
  → JSON-ответ
  → setGame()
  → обновлённое поле
```

[Вернуться к README](../README.md).
