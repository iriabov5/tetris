package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Проверяет описание действующего HTTP-контракта и экспорт YAML. */
@DisplayName("OpenAPI — контракт работающего API")
class OpenApiContractHttpTest extends GameHttpSupport {
    /** Создаёт набор проверок runtime-контракта. */
    OpenApiContractHttpTest() {
    }

    /**
     * Проверяет два формата и экспорт без второго вручную написанного контракта.
     *
     * @throws Exception при ошибке HTTP или чтения артефакта
     */
    @Test
    @DisplayName("JSON и YAML равны экспортированному контракту")
    void matchesExportedContract() throws Exception {
        // Arrange
        GameApiClient api = client();
        Path artifact = Path.of("../../docs/openapi.yaml");

        // Act
        var json = api.request("GET", "/v3/api-docs", null, null);
        var yaml = api.request("GET", "/v3/api-docs.yaml", null, null);
        JsonNode runtime = OpenApiArtifact.parse(json.body());

        // Assert
        assertEquals(200, json.statusCode());
        assertEquals(200, yaml.statusCode());
        assertTrue(json.headers().firstValue("Content-Type").orElseThrow().contains("json"));
        assertTrue(yaml.headers().firstValue("Content-Type").orElseThrow()
                .startsWith("application/vnd.oai.openapi"));
        assertEquals(runtime, OpenApiArtifact.parse(yaml.body()));
        if (Boolean.getBoolean("tetris.openapi.export")) {
            OpenApiArtifact.publish(artifact, yaml.body());
        }
        String difference = OpenApiArtifact.difference(OpenApiArtifact.parse(Files.readString(artifact)), runtime, "");
        assertEquals("", difference, "Контракт расходится в " + difference + ". Выполните make openapi");
    }

    /**
     * Проверяет контрактные ограничения, которые невозможно вывести только из типов.
     *
     * @throws Exception при ошибке HTTP или разбора JSON
     */
    @Test
    @DisplayName("Описывает обязательные поля, nullable и границы геометрии")
    void describesModelConstraints() throws Exception {
        // Arrange
        GameApiClient api = client();

        // Act
        JsonNode doc = api.json(api.request("GET", "/v3/api-docs", null, null));
        JsonNode schemas = doc.at("/components/schemas");

        // Assert
        assertEquals("Tetris API", doc.at("/info/title").asText());
        assertEquals("/", doc.at("/servers/0/url").asText());
        assertEquals(3, doc.path("paths").size());
        assertEquals("uuid", schemas.at("/GameSnapshot/properties/gameId/format").asText());
        assertTrue(schemas.at("/GameSnapshot/properties/active/nullable").asBoolean());
        assertTrue(strings(schemas.at("/GameSnapshot/required")).contains("active"));
        assertEquals(1, schemas.at("/GameSnapshot/properties/width/minimum").asInt());
        assertEquals(1, schemas.at("/GameSnapshot/properties/height/minimum").asInt());
        assertEquals(0, schemas.at("/GameSnapshot/properties/clearedLines/minimum").asInt(-1));
        assertEquals(0, schemas.at("/Cell/properties/x/minimum").asInt(-1));
        assertEquals(0, schemas.at("/Cell/properties/y/minimum").asInt(-1));
        assertEquals(4, schemas.at("/ActiveView/properties/cells/minItems").asInt());
        assertEquals(4, schemas.at("/ActiveView/properties/cells/maxItems").asInt());
        assertEquals(Set.of("LEFT", "RIGHT", "ROTATE", "DOWN", "HARD_DROP"),
                strings(schemas.at("/CommandBody/properties/command/enum")));
        assertEquals(Set.of("command"), strings(schemas.at("/CommandBody/required")));
        assertEquals(Set.of("APPLIED", "BLOCKED", "FINISHED"),
                strings(schemas.at("/CommandOutcome/properties/result/enum")));
    }

    /**
     * Сопоставляет реальный успешный ответ и ошибки с описанными статусами и типами.
     *
     * @throws Exception при ошибке HTTP или разбора JSON
     */
    @Test
    @DisplayName("Описывает Location и применимые ошибки игровых операций")
    void describesOperationResponses() throws Exception {
        // Arrange
        GameApiClient api = client();
        JsonNode doc = api.json(api.request("GET", "/v3/api-docs", null, null));
        JsonNode creation = doc.at("/paths/~1api~1games/post");
        JsonNode reading = doc.at("/paths/~1api~1games~1{id}/get");
        JsonNode command = doc.at("/paths/~1api~1games~1{id}~1commands/post");

        // Act
        var created = api.create();
        String id = api.json(created).path("gameId").asText();
        var invalid = api.request("POST", "/api/games/" + id + "/commands", "{}", "application/json");

        // Assert
        assertEquals("createGame", creation.path("operationId").asText());
        assertEquals("readGame", reading.path("operationId").asText());
        assertEquals("executeCommand", command.path("operationId").asText());
        assertTrue(creation.at("/responses/201/headers/Location").isObject());
        assertTrue(created.headers().firstValue("Location").isPresent());
        assertEquals(201, created.statusCode());
        api.assertProblem(400, invalid);
        assertEquals(Set.of("201", "500"), keys(creation.path("responses")));
        assertEquals(Set.of("200", "400", "404"), keys(reading.path("responses")));
        assertEquals(Set.of("200", "400", "404", "415", "500"), keys(command.path("responses")));
        assertTrue(command.at("/requestBody/required").asBoolean());
        for (String name : Set.of("BadRequest", "NotFound", "UnsupportedMediaType", "OperationFailed")) {
            assertTrue(doc.at("/components/responses/" + name + "/content/application~1problem+json").isObject());
        }
    }

    /**
     * Собирает строковые значения массива независимо от порядка.
     *
     * @param values массив строк
     * @return множество значений
     */
    private static Set<String> strings(JsonNode values) {
        Set<String> result = new HashSet<>();
        values.forEach(value -> result.add(value.asText()));
        return result;
    }

    /**
     * Собирает имена свойств JSON-объекта.
     *
     * @param object объект ответа
     * @return множество имён
     */
    private static Set<String> keys(JsonNode object) {
        Set<String> result = new HashSet<>();
        object.fieldNames().forEachRemaining(result::add);
        return result;
    }
}
