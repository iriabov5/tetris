package dev.tetris.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Проверяет обнаружение дрейфа и сохранение артефакта при ошибке. */
@DisplayName("OpenAPI — защита экспортированного контракта")
class OpenApiArtifactTest {
    /** Создаёт независимый набор файловых проверок. */
    OpenApiArtifactTest() {
    }

    /**
     * Обнаруживает удаление команды с указанием пути, не перезаписывая файл.
     *
     * @throws Exception при ошибке чтения контракта
     */
    @Test
    @DisplayName("Обнаруживает удаление HARD_DROP из копии контракта")
    void detectsRemovedCommand() throws Exception {
        // Arrange
        var expected = OpenApiArtifact.parse(Files.readString(Path.of("../../docs/openapi.yaml")));
        var changed = expected.deepCopy();
        var command = (ObjectNode) changed.at("/components/schemas/CommandBody/properties/command");
        command.putArray("enum").add("LEFT").add("RIGHT").add("ROTATE").add("DOWN");

        // Act
        String difference = OpenApiArtifact.difference(expected, changed, "");

        // Assert
        assertEquals("/components/schemas/CommandBody/properties/command/enum", difference);
        assertEquals(5, expected.at("/components/schemas/CommandBody/properties/command/enum").size());
    }

    /**
     * Сравнивает порядок ключей и элементов required без ложного расхождения.
     *
     * @throws Exception при ошибке разбора документов
     */
    @Test
    @DisplayName("Игнорирует форматирование, порядок ключей и элементов required")
    void ignoresNonSemanticOrdering() throws Exception {
        // Arrange
        String yaml = "openapi: 3.0.1\npaths: { /example: { required: [a, b] } }\ncomponents: {}";
        String json = "{\"components\":{},\"paths\":{\"/example\":{\"required\":[\"b\",\"a\"]}},\"openapi\":\"3.0.1\"}";

        // Act
        String difference = OpenApiArtifact.difference(OpenApiArtifact.parse(yaml), OpenApiArtifact.parse(json), "");

        // Assert
        assertEquals("", difference);
    }

    /**
     * Не заменяет предыдущий контракт невалидным ответом сервера.
     *
     * @param directory независимый временный каталог
     * @throws Exception при ошибке работы с временным файлом
     */
    @Test
    @DisplayName("Сохраняет прежний YAML при невалидном ответе экспорта")
    void preservesArtifactOnInvalidExport(@TempDir Path directory) throws Exception {
        // Arrange
        Path artifact = directory.resolve("openapi.yaml");
        Files.writeString(artifact, "previous contract");

        // Act
        IOException failure = assertThrows(IOException.class, () -> OpenApiArtifact.publish(artifact, "{}"));

        // Assert
        assertTrue(failure.getMessage().contains("OpenAPI"));
        assertEquals("previous contract", Files.readString(artifact));
    }
}
