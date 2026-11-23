package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP — создание партии.
 */
@DisplayName("HTTP — создание партии")
class GameCreationHttpTest extends GameHttpSupport {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    GameCreationHttpTest() {
    }

    /**
     * Возвращает 201, адрес партии и начальное состояние 10 на 20.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Возвращает 201, адрес партии и начальное состояние 10 на 20")
    void returnsInitialStateAndLocation() throws Exception {
        // Arrange
        GameApiClient api = client();

        // Act
        HttpResponse<String> response = api.create();

        // Assert
        JsonNode state = api.json(response);
        assertEquals(201, response.statusCode());
        assertEquals("/api/games/" + state.get("gameId").asText(),
                response.headers().firstValue("Location").orElseThrow());
        assertEquals(10, state.get("width").asInt());
        assertEquals(20, state.get("height").asInt());
        assertEquals("RUNNING", state.get("status").asText());
        assertEquals(0, state.get("clearedLines").asInt());
        assertEquals(4, state.at("/active/cells").size());
        assertTrue(state.get("settled").isEmpty());
    }

    /**
     * Создаёт разные идентификаторы для двух партий.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Создаёт разные идентификаторы для двух партий")
    void createsDifferentIdentifiers() throws Exception {
        // Arrange
        GameApiClient api = client();
        String firstId = api.json(api.create()).get("gameId").asText();

        // Act
        String secondId = api.json(api.create()).get("gameId").asText();

        // Assert
        assertNotEquals(firstId, secondId);
    }

}
