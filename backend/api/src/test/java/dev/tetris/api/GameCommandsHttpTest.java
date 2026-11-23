package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;
import dev.tetris.core.Cell;
import dev.tetris.core.Command;
import dev.tetris.core.TetrisGame;
import dev.tetris.core.piece.T;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP — точная геометрия результата команд и независимость партий.
 */
@DisplayName("HTTP — выполнение команд и независимость партий")
@ActiveProfiles("e2e")
@Import(GameCommandsHttpTest.CommandGameConfiguration.class)
class GameCommandsHttpTest extends GameHttpSupport {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    GameCommandsHttpTest() {
    }

    /**
     * Проверяет реальные координаты после каждой команды и сохранение результата сервером.
     *
     * @param command команда проверяемого сценария
     * @param expectedCells вручную заданные ожидаемые клетки фигуры T
     * @throws Exception при ошибке HTTP-запроса или чтения ответа
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("commandCases")
    @DisplayName("Команда возвращает ожидаемые координаты и сохраняет их в партии")
    void executesCommand(Command command, Set<Cell> expectedCells) throws Exception {
        // Arrange
        GameApiClient api = client();
        JsonNode before = api.json(api.create());
        String id = before.get("gameId").asText();

        // Act
        HttpResponse<String> response = api.command(id, command.name());

        // Assert
        JsonNode outcome = api.json(response);
        JsonNode state = outcome.get("state");
        assertEquals(200, response.statusCode());
        assertEquals("APPLIED", outcome.get("result").asText());
        assertEquals(expectedCells, activeCells(state));
        assertEquals(before.get("settled"), state.get("settled"));
        assertEquals(before.get("clearedLines"), state.get("clearedLines"));
        assertEquals(state, api.json(api.read(id)));
    }

    /**
     * Сохраняет вторую партию при команде для первой.
     *
     * @throws Exception при ошибке HTTP-запроса или чтения ответа
     */
    @Test
    @DisplayName("Сохраняет вторую партию при команде для первой")
    void keepsOtherGameUnchanged() throws Exception {
        // Arrange
        GameApiClient api = client();
        String firstId = api.json(api.create()).get("gameId").asText();
        JsonNode second = api.json(api.create());

        // Act
        api.command(firstId, "DOWN");

        // Assert
        assertEquals(second, api.json(api.read(second.get("gameId").asText())));
    }

    /**
     * Задаёт ожидаемые абсолютные координаты без вызова алгоритмов перемещения и поворота ядра.
     *
     * @return четыре именованных случая для фигуры T на поле 6 × 6
     */
    static Stream<Arguments> commandCases() {
        return Stream.of(
                Arguments.of(Command.LEFT, Set.of(
                        new Cell(1, 0), new Cell(0, 1), new Cell(1, 1), new Cell(2, 1))),
                Arguments.of(Command.RIGHT, Set.of(
                        new Cell(3, 0), new Cell(2, 1), new Cell(3, 1), new Cell(4, 1))),
                Arguments.of(Command.DOWN, Set.of(
                        new Cell(2, 1), new Cell(1, 2), new Cell(2, 2), new Cell(3, 2))),
                Arguments.of(Command.ROTATE, Set.of(
                        new Cell(3, 1), new Cell(2, 0), new Cell(2, 1), new Cell(2, 2))));
    }

    /**
     * Читает геометрию из JSON, не полагаясь на порядок клеток в ответе.
     *
     * @param state снимок партии, возвращённый HTTP API
     * @return координаты активной фигуры
     */
    private static Set<Cell> activeCells(JsonNode state) {
        Set<Cell> cells = new HashSet<>();
        for (JsonNode cell : state.at("/active/cells")) {
            cells.add(new Cell(cell.get("x").asInt(), cell.get("y").asInt()));
        }
        return cells;
    }

    /**
     * Изолированная фабрика T позволяет проверить изменение геометрии при повороте по HTTP.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class CommandGameConfiguration {
        /**
         * Создаёт конфигурацию предсказуемой геометрии HTTP-проверок команд.
         */
        CommandGameConfiguration() {
        }

        /**
         * Переопределяет выбор фабрики только для этого тестового контекста.
         *
         * @return фабрика отдельных партий 6 × 6 с фигурами T
         */
        @Bean
        @Primary
        Supplier<TetrisGame> commandGameFactory() {
            return () -> new TetrisGame(6, 6, T::new);
        }
    }
}
