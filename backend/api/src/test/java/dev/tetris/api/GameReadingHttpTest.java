package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP — чтение партии.
 */
@DisplayName("HTTP — чтение партии")
class GameReadingHttpTest extends GameHttpSupport {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    GameReadingHttpTest() {
    }

    /**
     * Возвращает снимок созданной партии.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Возвращает снимок созданной партии")
    void readsCreatedSnapshot() throws Exception {
        // Arrange
        GameApiClient api = client();
        JsonNode created = api.json(api.create());

        // Act
        HttpResponse<String> response = api.read(created.get("gameId").asText());

        // Assert
        assertEquals(200, response.statusCode());
        assertEquals(created, api.json(response));
    }

    /**
     * Возвращает 404 для неизвестного идентификатора.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Возвращает 404 для неизвестного идентификатора")
    void rejectsMissingGame() throws Exception {
        // Arrange
        GameApiClient api = client();
        String missingId = UUID.randomUUID().toString();

        // Act
        HttpResponse<String> response = api.read(missingId);

        // Assert
        api.assertProblem(404, response);
    }

    /**
     * Возвращает 400 для некорректного UUID.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Возвращает 400 для некорректного UUID")
    void rejectsInvalidUuid() throws Exception {
        // Arrange
        GameApiClient api = client();

        // Act
        HttpResponse<String> response = api.read("not-a-uuid");

        // Assert
        api.assertProblem(400, response);
    }

}
