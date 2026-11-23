package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP — завершённая партия и тестовый профиль.
 */
@DisplayName("HTTP — завершённая партия и тестовый профиль")
@ActiveProfiles("e2e")
class GameFinishedHttpTest extends GameHttpSupport {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    GameFinishedHttpTest() {
    }

    /**
     * Профиль e2e создаёт O на поле 4 на 4.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Профиль e2e создаёт O на поле 4 на 4")
    void usesScriptedProfile() throws Exception {
        // Arrange
        GameApiClient api = client();

        // Act
        JsonNode state = api.json(api.create());

        // Assert
        assertEquals(4, state.get("width").asInt());
        assertEquals(4, state.get("height").asInt());
        assertEquals("O", state.at("/active/kind").asText());
    }

    /**
     * Возвращает FINISHED и сохраняет снимок завершённой партии.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Возвращает FINISHED и сохраняет снимок завершённой партии")
    void returnsFinishedWithoutChanges() throws Exception {
        // Arrange
        GameApiClient api = client();
        String id = api.json(api.create()).get("gameId").asText();
        for (int step = 0; step < 4; step++) {
            api.command(id, "DOWN");
        }
        JsonNode before = api.json(api.read(id));

        // Act
        HttpResponse<String> response = api.command(id, "ROTATE");

        // Assert
        JsonNode outcome = api.json(response);
        assertEquals("GAME_OVER", before.get("status").asText());
        assertTrue(before.get("active").isNull());
        assertEquals("FINISHED", outcome.get("result").asText());
        assertEquals(before, outcome.get("state"));
    }

}
