package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import dev.tetris.core.Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP — мгновенный сброс одной командой и сохранение результата.
 */
@DisplayName("HTTP — мгновенный сброс")
@ActiveProfiles("e2e")
class GameHardDropHttpTest extends GameHttpSupport {
    /**
     * Создаёт группу проверок новой команды публичного API.
     */
    GameHardDropHttpTest() {
    }

    /**
     * Проверяет координаты закреплённой фигуры, следующую фигуру и последующее чтение.
     *
     * @throws Exception при ошибке HTTP-запроса или разбора JSON
     */
    @Test
    @DisplayName("Один POST HARD_DROP закрепляет фигуру у дна и сохраняет снимок")
    void dropsWithOneRequest() throws Exception {
        // Arrange
        GameApiClient api = client();
        String id = api.json(api.create()).get("gameId").asText();

        // Act
        HttpResponse<String> response = api.command(id, "HARD_DROP");

        // Assert
        assertEquals(200, response.statusCode());
        JsonNode outcome = api.json(response);
        JsonNode state = outcome.get("state");
        Set<Cell> settled = new HashSet<>();
        for (JsonNode cell : state.get("settled")) settled.add(new Cell(cell.get("x").asInt(), cell.get("y").asInt()));
        assertEquals("APPLIED", outcome.get("result").asText());
        assertEquals(Set.of(new Cell(1, 2), new Cell(2, 2), new Cell(1, 3), new Cell(2, 3)), settled);
        assertEquals(4, state.at("/active/cells").size());
        assertEquals(state, api.json(api.read(id)));
    }
}
