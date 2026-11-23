package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Выполняет настоящие HTTP-запросы и читает ответы без подмены серверных компонентов.
 */
final class GameApiClient {
    /**
     * Адрес сервера текущего тестового контекста.
     */
    private final String baseUrl;
    /**
     * Клиент, пригодный для параллельных HTTP-запросов.
     */
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    /**
     * Преобразователь JSON-ответов в значения для проверок.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Привязывает клиент к порту поднятого тестового сервера.
     *
     * @param port локальный порт сервера
     */
    GameApiClient(int port) {
        baseUrl = "http://127.0.0.1:" + port;
    }

    /**
     * Отправляет запрос с явно заданным телом для проверки HTTP-контракта.
     *
     * @param method      метод HTTP
     * @param path        путь относительно сервера
     * @param body        тело или null для запроса без тела
     * @param contentType тип содержимого или null для отсутствия заголовка
     * @return полный HTTP-ответ
     * @throws Exception при ошибке передачи или прерывании запроса
     */
    HttpResponse<String> request(String method, String path, String body, String contentType) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10));
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        var publisher = body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        request.method(method, publisher);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Создаёт новую партию через публичную операцию API.
     *
     * @return ответ POST /api/games
     * @throws Exception при ошибке HTTP-запроса
     */
    HttpResponse<String> create() throws Exception {
        return request("POST", "/api/games", null, null);
    }

    /**
     * Читает снимок партии через публичную операцию API.
     *
     * @param id идентификатор проверяемой партии
     * @return ответ GET /api/games/{id}
     * @throws Exception при ошибке HTTP-запроса
     */
    HttpResponse<String> read(String id) throws Exception {
        return request("GET", "/api/games/" + id, null, null);
    }

    /**
     * Отправляет команду с обычным JSON-телом.
     *
     * @param id      идентификатор партии
     * @param command имя команды
     * @return результат выполнения и снимок партии
     * @throws Exception при ошибке HTTP-запроса
     */
    HttpResponse<String> command(String id, String command) throws Exception {
        return request("POST", "/api/games/" + id + "/commands",
                "{\"command\":\"" + command + "\"}", "application/json");
    }

    /**
     * Разбирает тело ответа для проверки полей контракта.
     *
     * @param response ответ сервера
     * @return JSON-дерево тела
     * @throws Exception при некорректном JSON
     */
    JsonNode json(HttpResponse<String> response) throws Exception {
        return mapper.readTree(response.body());
    }

    /**
     * Проверяет единый формат ошибок API.
     *
     * @param expectedStatus ожидаемый HTTP-статус
     * @param response       ответ проверяемой операции
     * @throws Exception при некорректном JSON ответа
     */
    void assertProblem(int expectedStatus, HttpResponse<String> response) throws Exception {
        assertEquals(expectedStatus, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow()
                .startsWith("application/problem+json"));
        JsonNode problem = json(response);
        assertEquals(expectedStatus, problem.get("status").asInt());
        assertTrue(problem.hasNonNull("title"));
    }
}
