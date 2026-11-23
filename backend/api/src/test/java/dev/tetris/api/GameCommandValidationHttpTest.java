package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP — ошибки входных команд.
 */
@DisplayName("HTTP — ошибки входных команд")
class GameCommandValidationHttpTest extends GameHttpSupport {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    GameCommandValidationHttpTest() {
    }

    /**
     * Возвращает 400 и сохраняет партию при некорректном теле команды.
     *
     * @param scenario имя ошибки ввода
     * @param body     тело запроса
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    @DisplayName("Возвращает 400 и сохраняет партию при некорректном теле команды")
    void rejectsInvalidBodyWithoutChanges(String scenario, String body) throws Exception {
        // Arrange
        GameApiClient api = client();
        JsonNode before = api.json(api.create());
        String id = before.get("gameId").asText();

        // Act
        HttpResponse<String> response = api.request("POST", "/api/games/" + id + "/commands", body, "application/json");

        // Assert
        api.assertProblem(400, response);
        assertEquals(before, api.json(api.read(id)));
    }

    /**
     * Возвращает 415 для текстового тела команды.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Возвращает 415 для текстового тела команды")
    void rejectsWrongContentType() throws Exception {
        // Arrange
        GameApiClient api = client();
        String id = api.json(api.create()).get("gameId").asText();

        // Act
        HttpResponse<String> response = api.request("POST", "/api/games/" + id + "/commands", "DOWN", "text/plain");

        // Assert
        api.assertProblem(415, response);
    }

    /**
     * Возвращает 404 при корректной команде для неизвестной партии.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Возвращает 404 при корректной команде для неизвестной партии")
    void rejectsCommandForMissingGame() throws Exception {
        // Arrange
        GameApiClient api = client();
        String missingId = UUID.randomUUID().toString();

        // Act
        HttpResponse<String> response = api.command(missingId, "DOWN");

        // Assert
        api.assertProblem(404, response);
    }

    /**
     * Проверяет тело команды до поиска неизвестной партии.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Проверяет тело команды до поиска неизвестной партии")
    void validatesBodyBeforeLookup() throws Exception {
        // Arrange
        GameApiClient api = client();
        String path = "/api/games/" + UUID.randomUUID() + "/commands";

        // Act
        HttpResponse<String> response = api.request("POST", path, "{}", "application/json");

        // Assert
        api.assertProblem(400, response);
    }

    /**
     * Возвращает 400 при некорректном UUID команды.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Возвращает 400 при некорректном UUID команды")
    void rejectsCommandWithInvalidUuid() throws Exception {
        // Arrange
        GameApiClient api = client();

        // Act
        HttpResponse<String> response = api.command("bad", "DOWN");

        // Assert
        api.assertProblem(400, response);
    }

    /**
     * Даёт каждому нарушению формата самостоятельное имя в отчёте.
     *
     * @return пары из описания ошибки и JSON-тела
     */
    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of("нет поля command", "{}"),
                Arguments.of("null вместо объекта", "null"),
                Arguments.of("повреждённый JSON", "{"),
                Arguments.of("null-команда", "{\"command\":null}"),
                Arguments.of("пустая команда", "{\"command\":\"\"}"),
                Arguments.of("неизвестная команда", "{\"command\":\"UNKNOWN\"}"),
                Arguments.of("пустое тело", ""));
    }
}
