package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

/** Семантическое сравнение и атомарная публикация экспортированного контракта. */
final class OpenApiArtifact {
    /** Преобразователь обоих представлений YAML и JSON. */
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    /** Запрещает создание экземпляра утилиты. */
    private OpenApiArtifact() {
    }

    /**
     * Разбирает контракт независимо от форматирования и порядка ключей.
     *
     * @param text текст YAML или JSON
     * @return нормализованный документ
     * @throws IOException если текст не является документом OpenAPI 3.0
     */
    static JsonNode parse(String text) throws IOException {
        JsonNode document = YAML.readTree(text);
        if (document == null || !document.path("openapi").asText().startsWith("3.0.")
                || !document.path("paths").isObject() || !document.path("components").isObject()) {
            throw new IOException("Ожидается документ OpenAPI 3.0 с paths и components");
        }
        return normalize(document, "");
    }

    /**
     * Копирует дерево, сортируя только массивы с семантикой множества.
     *
     * @param node исходный узел
     * @param property имя свойства родителя
     * @return независимое дерево для сравнения
     */
    private static JsonNode normalize(JsonNode node, String property) {
        if (node.isObject()) {
            ObjectNode result = YAML.createObjectNode();
            node.properties().forEach(entry -> result.set(entry.getKey(), normalize(entry.getValue(), entry.getKey())));
            return result;
        }
        if (node.isArray()) {
            var elements = new ArrayList<JsonNode>();
            node.forEach(element -> elements.add(normalize(element, "")));
            if (property.equals("required") || property.equals("enum")) {
                elements.sort(Comparator.comparing(JsonNode::toString));
            }
            ArrayNode result = YAML.createArrayNode();
            elements.forEach(result::add);
            return result;
        }
        return node.deepCopy();
    }

    /**
     * Возвращает первый отличающийся JSON Pointer, включая отсутствующие поля.
     *
     * @param expected ожидаемое дерево
     * @param actual фактическое дерево
     * @param pointer путь текущего узла
     * @return путь отличия либо пустая строка при равенстве
     */
    static String difference(JsonNode expected, JsonNode actual, String pointer) {
        if (expected.equals(actual)) {
            return "";
        }
        if (expected.isObject() && actual.isObject()) {
            var keys = new TreeSet<String>();
            expected.fieldNames().forEachRemaining(keys::add);
            actual.fieldNames().forEachRemaining(keys::add);
            for (String key : keys) {
                String escaped = key.replace("~", "~0").replace("/", "~1");
                String child = difference(expected.path(key), actual.path(key), pointer + "/" + escaped);
                if (!child.isEmpty()) {
                    return child;
                }
            }
        }
        if (expected.isArray() && actual.isArray() && expected.size() == actual.size()) {
            for (int i = 0; i < expected.size(); i++) {
                String child = difference(expected.get(i), actual.get(i), pointer + "/" + i);
                if (!child.isEmpty()) {
                    return child;
                }
            }
        }
        return pointer.isEmpty() ? "/" : pointer;
    }

    /**
     * Проверяет документ до записи и заменяет артефакт одной атомарной операцией.
     *
     * @param target существующий путь артефакта
     * @param text полученный от сервера YAML
     * @throws IOException если документ невалиден или атомарная запись невозможна
     */
    static void publish(Path target, String text) throws IOException {
        parse(text);
        Path temporary = Files.createTempFile(target.toAbsolutePath().getParent(), "openapi-", ".yaml");
        try {
            Files.writeString(temporary, text);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
