package dev.tetris.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.tetris.core.Command;
import dev.tetris.core.TetrisGame;
import dev.tetris.core.piece.O;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Журнал API — уровни, контекст партии, фильтрация и однократное окончание.
 */
@DisplayName("Журнал API — уровни и контекст событий")
class GameLoggingTest {
    /**
     * Реальные события Logback, записанные во время одного сценария.
     */
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    /**
     * Исходные уровни логгеров для восстановления после сценария.
     */
    private final Map<Logger, Level> originalLevels = new HashMap<>();
    /**
     * Исходная передача событий родительским логгерам.
     */
    private final Map<Logger, Boolean> originalAdditivity = new HashMap<>();

    /**
     * Создаёт независимую группу проверок настоящих событий журнала.
     */
    GameLoggingTest() {
    }

    /**
     * Подключает захват к логгерам API без подмены самих операций игры.
     */
    @BeforeEach
    void captureEvents() {
        appender.start();
        for (Class<?> type : List.of(GameRegistry.class, GameController.class, ApiErrors.class)) {
            Logger logger = (Logger) LoggerFactory.getLogger(type);
            originalLevels.put(logger, logger.getLevel());
            originalAdditivity.put(logger, logger.isAdditive());
            logger.setLevel(Level.DEBUG);
            logger.setAdditive(false);
            logger.addAppender(appender);
        }
    }

    /**
     * Отсоединяет захват и возвращает настройки логгеров другим тестам.
     */
    @AfterEach
    void restoreLoggers() {
        originalLevels.forEach((logger, level) -> {
            logger.detachAppender(appender);
            logger.setLevel(level);
            logger.setAdditive(originalAdditivity.get(logger));
        });
        appender.stop();
    }

    /**
     * Записывает UUID и размеры новой партии на уровне INFO.
     */
    @Test
    @DisplayName("Создание записывает одно INFO-событие с UUID и размерами")
    void logsCreation() {
        // Arrange
        GameRegistry registry = registry();

        // Act
        UUID id = registry.create().snapshot().gameId();

        // Assert
        List<ILoggingEvent> created = events(Level.INFO, "game_created");
        assertEquals(1, created.size());
        assertTrue(created.getFirst().getFormattedMessage().contains("gameId=" + id));
        assertTrue(created.getFirst().getFormattedMessage().contains("width=4 height=4"));
    }

    /**
     * Различает применённую и заблокированную команды на уровне DEBUG.
     */
    @Test
    @DisplayName("DEBUG содержит команду, результат, UUID и счётчик без WARN для BLOCKED")
    void logsAppliedAndBlockedCommands() {
        // Arrange
        GameRegistry registry = registry();
        UUID id = registry.create().snapshot().gameId();
        GameController controller = new GameController(registry);

        // Act
        controller.command(id, new GameController.CommandBody(Command.LEFT));
        controller.command(id, new GameController.CommandBody(Command.LEFT));

        // Assert
        List<ILoggingEvent> commands = events(Level.DEBUG, "game_command");
        assertEquals(2, commands.size());
        assertTrue(commands.get(0).getFormattedMessage().contains("command=LEFT result=APPLIED clearedLines=0"));
        assertTrue(commands.get(1).getFormattedMessage().contains("command=LEFT result=BLOCKED clearedLines=0"));
        assertTrue(commands.stream().allMatch(event -> event.getFormattedMessage().contains("gameId=" + id)));
        assertTrue(appender.list.stream().noneMatch(event -> event.getLevel().equals(Level.WARN)));
    }

    /**
     * Проверяет фильтрацию автоматических шагов настройкой INFO.
     */
    @Test
    @DisplayName("При INFO автоматический DOWN не попадает в журнал команд")
    void suppressesCommandsAtInfoLevel() {
        // Arrange
        GameRegistry registry = registry();
        UUID id = registry.create().snapshot().gameId();
        GameController controller = new GameController(registry);
        ((Logger) LoggerFactory.getLogger(GameController.class)).setLevel(Level.INFO);

        // Act
        controller.command(id, new GameController.CommandBody(Command.DOWN));

        // Assert
        assertEquals(1, events(Level.INFO, "game_created").size());
        assertTrue(events(Level.DEBUG, "game_command").isEmpty());
    }

    /**
     * Не повторяет событие окончания при командах уже завершённой партии.
     */
    @Test
    @DisplayName("Окончание записывается один раз, повторный FINISHED остаётся на DEBUG")
    void logsFinishOnlyOnce() {
        // Arrange
        GameRegistry registry = registry();
        UUID id = registry.create().snapshot().gameId();
        GameController controller = new GameController(registry);
        for (int step = 0; step < 4; step++) {
            controller.command(id, new GameController.CommandBody(Command.DOWN));
        }

        // Act
        controller.command(id, new GameController.CommandBody(Command.ROTATE));

        // Assert
        List<ILoggingEvent> finished = events(Level.INFO, "game_finished");
        assertEquals(1, finished.size());
        assertTrue(finished.getFirst().getFormattedMessage().contains("gameId=" + id));
        assertTrue(events(Level.DEBUG, "game_command").getLast().getFormattedMessage().contains("result=FINISHED"));
    }

    /**
     * Сопровождает отсутствие партии предупреждением с искомым UUID.
     */
    @Test
    @DisplayName("Неизвестная партия даёт WARN с UUID и сохраняет исключение")
    void warnsAboutMissingGame() {
        // Arrange
        GameRegistry registry = registry();
        UUID id = UUID.randomUUID();

        // Act
        assertThrows(GameNotFoundException.class, () -> registry.get(id));

        // Assert
        List<ILoggingEvent> warnings = events(Level.WARN, "game_missing");
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().getFormattedMessage().contains("gameId=" + id));
    }

    /**
     * Не раскрывает тело запроса при предупреждении об отсутствующей команде.
     */
    @Test
    @DisplayName("Отсутствующая команда даёт WARN с причиной и прежний HTTP 400")
    void warnsAboutMissingCommand() {
        // Arrange
        GameRegistry registry = registry();
        UUID id = registry.create().snapshot().gameId();
        GameController controller = new GameController(registry);
        GameController.CommandBody body = new GameController.CommandBody(null);

        // Act
        var failure = assertThrows(ResponseStatusException.class, () -> controller.command(id, body));

        // Assert
        assertEquals(400, failure.getStatusCode().value());
        List<ILoggingEvent> warnings = events(Level.WARN, "command_rejected");
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().getFormattedMessage().contains("gameId=" + id + " reason=missing_command"));
    }

    /**
     * Сохраняет ошибку фабрики и записывает её стек на ERROR.
     */
    @Test
    @DisplayName("Сбой создания даёт ERROR со стеком и исходным исключением")
    void logsFactoryFailure() {
        // Arrange
        IllegalStateException expected = new IllegalStateException("Фабрика недоступна");
        GameRegistry registry = new GameRegistry(() -> { throw expected; });

        // Act
        var actual = assertThrows(GameOperationException.class, registry::create);
        var problem = new ApiErrors().failedOperation(actual);

        // Assert
        assertSame(expected, actual.getCause());
        assertEquals(500, problem.getStatus());
        List<ILoggingEvent> errors = events(Level.ERROR, "game_creation_failed");
        assertEquals(1, errors.size());
        assertNotNull(errors.getFirst().getThrowableProxy());
        assertEquals(expected.getMessage(), errors.getFirst().getThrowableProxy().getCause().getMessage());
    }

    /**
     * Сохраняет ошибку источника и пишет UUID, команду и стек на ERROR.
     */
    @Test
    @DisplayName("Сбой команды даёт ERROR с UUID, командой и исходным исключением")
    void logsCommandFailure() {
        // Arrange
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException expected = new IllegalStateException("Источник недоступен");
        GameRegistry registry = new GameRegistry(() -> new TetrisGame(4, 4, () -> {
            if (calls.incrementAndGet() == 2) throw expected;
            return new O();
        }));
        UUID id = registry.create().snapshot().gameId();
        GameController controller = new GameController(registry);
        GameController.CommandBody body = new GameController.CommandBody(Command.DOWN);
        controller.command(id, body);
        controller.command(id, body);

        // Act
        var actual = assertThrows(GameOperationException.class, () -> controller.command(id, body));
        var problem = new ApiErrors().failedOperation(actual);

        // Assert
        assertSame(expected, actual.getCause());
        assertEquals(500, problem.getStatus());
        List<ILoggingEvent> errors = events(Level.ERROR, "game_command_failed");
        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().getFormattedMessage().contains("gameId=" + id + " command=DOWN"));
        assertNotNull(errors.getFirst().getThrowableProxy());
    }

    /**
     * Отбирает события по уровню и точному имени, не смешивая ошибку команды с её результатом.
     *
     * @param level проверяемый уровень
     * @param name имя события
     * @return события текущего сценария
     */
    private List<ILoggingEvent> events(Level level, String name) {
        return appender.list.stream()
                .filter(event -> event.getLevel().equals(level))
                .filter(event -> event.getFormattedMessage().equals("event=" + name)
                        || event.getFormattedMessage().startsWith("event=" + name + " "))
                .toList();
    }

    /**
     * Создаёт независимый реестр предсказуемых партий без Spring-контекста.
     *
     * @return реестр партий 4 × 4 с фигурами O
     */
    private static GameRegistry registry() {
        return new GameRegistry(() -> new TetrisGame(4, 4, O::new));
    }
}
