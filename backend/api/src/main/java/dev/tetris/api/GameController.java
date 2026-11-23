package dev.tetris.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import dev.tetris.core.Command;
import dev.tetris.core.CommandOutcome;
import dev.tetris.core.GameSnapshot;
import dev.tetris.core.CommandResult;
import dev.tetris.core.GameStatus;
import dev.tetris.core.TetrisGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * HTTP-операции создания партии, чтения снимка и выполнения одной команды.
 */
@RestController
@RequestMapping(value = "/api/games", produces = MediaType.APPLICATION_JSON_VALUE)
public class GameController {
    /**
     * Журнал результатов команд без раскрытия клеток и тела запроса.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GameController.class);

    /**
     * Реестр, используемый операциями контроллера.
     */
    private final GameRegistry games;

    /**
     * Связывает HTTP-операции с реестром партий.
     *
     * @param games реестр текущего процесса
     */
    public GameController(GameRegistry games) {
        this.games = games;
    }

    /**
     * Регистрирует новую партию и указывает адрес её чтения.
     *
     * @return HTTP 201 со снимком и заголовком Location
     */
    @Operation(operationId = "createGame", summary = "Создать независимую партию 10 на 20")
    @PostMapping
    public ResponseEntity<GameSnapshot> create() {
        GameSnapshot snapshot = games.create().snapshot();
        return ResponseEntity.created(URI.create("/api/games/" + snapshot.gameId())).body(snapshot);
    }

    /**
     * Читает снимок партии через синхронизированную операцию ядра.
     *
     * @param id идентификатор партии из пути
     * @return неизменяемое текущее состояние
     * @throws GameNotFoundException если партия отсутствует
     */
    @Operation(operationId = "readGame", summary = "Прочитать текущий снимок партии")
    @GetMapping("/{id}")
    public GameSnapshot read(@PathVariable UUID id) {
        return games.get(id).snapshot();
    }

    /**
     * Проверяет обязательность команды до поиска и изменения партии.
     *
     * @param id   идентификатор партии из пути
     * @param body JSON-тело с командой
     * @return результат ядра со снимком после команды
     * @throws ResponseStatusException если обязательная команда отсутствует
     * @throws GameNotFoundException   если партия отсутствует
     * @throws GameOperationException если команда завершилась ошибкой; контекст передаётся обработчику
     */
    @Operation(operationId = "executeCommand", summary = "Выполнить одну команду",
            description = "Каждый POST выполняет отдельное действие. DOWN делает шаг вниз или закрепляет фигуру. "
                    + "HARD_DROP сразу опускает и закрепляет фигуру, очищает строки и выпускает следующую. "
                    + "BLOCKED — штатный результат HTTP 200. Повтор POST выполняет новое действие.")
    @PostMapping(value = "/{id}/commands", consumes = MediaType.APPLICATION_JSON_VALUE)
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

    /**
     * Тело команды, не предоставляющее клиенту прямую запись клеток поля.
     *
     * @param command действие, которое должен выполнить сервер
     */
    public record CommandBody(@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Действие активной фигуры", example = "HARD_DROP") Command command) {
    }
}
