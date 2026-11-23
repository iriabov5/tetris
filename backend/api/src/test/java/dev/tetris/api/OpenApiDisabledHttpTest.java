package dev.tetris.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Проверяет независимость игрового API от включения документации. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
@DisplayName("OpenAPI — выключенная документация")
class OpenApiDisabledHttpTest extends GameHttpSupport {
    /** Создаёт набор проверок выключенной документации. */
    OpenApiDisabledHttpTest() {
    }

    /**
     * Скрывает документацию без отключения игровых команд.
     *
     * @throws Exception при ошибке HTTP или разбора JSON
     */
    @Test
    @DisplayName("Возвращает 404 для UI и схемы, сохраняя игровые операции")
    void disablesDocumentationOnly() throws Exception {
        // Arrange
        GameApiClient api = client();

        Map<String, Integer> statuses = new LinkedHashMap<>();

        // Act
        for (String path : new String[]{"/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs.yaml"}) {
            statuses.put(path, api.request("GET", path, null, null).statusCode());
        }
        var created = api.create();
        var command = api.command(api.json(created).path("gameId").asText(), "LEFT");

        // Assert
        statuses.forEach((path, status) -> assertEquals(404, status, path));
        assertEquals(201, created.statusCode());
        assertEquals(200, command.statusCode());
        assertEquals("APPLIED", api.json(command).path("result").asText());
    }
}
