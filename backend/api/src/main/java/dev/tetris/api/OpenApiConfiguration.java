package dev.tetris.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Дополняет выведенный из HTTP-моделей контракт без зависимостей Swagger в ядре.
 */
@Configuration
public class OpenApiConfiguration {
    /** Имя свойства статуса в снимке и описании ошибки. */
    private static final String STATUS_PROPERTY = "status";
    /** Имя допускающего null свойства активной фигуры. */
    private static final String ACTIVE_PROPERTY = "active";
    /** Имя общего ответа о некорректном запросе. */
    private static final String BAD_REQUEST_RESPONSE = "BadRequest";
    /** Имя общего ответа об отсутствующей партии. */
    private static final String NOT_FOUND_RESPONSE = "NotFound";
    /** Имя общего ответа о сбое операции партии. */
    private static final String OPERATION_FAILED_RESPONSE = "OperationFailed";

    /** Создаёт конфигурацию документации API. */
    public OpenApiConfiguration() {
        // Spring создаёт конфигурацию без зависимостей; компоненты определены методами @Bean.
    }

    /**
     * Задаёт название и относительный сервер для любого локального порта.
     *
     * @return метаданные и переиспользуемые ответы ошибок
     */
    @Bean
    public OpenAPI tetrisOpenApi() {
        var components = new Components()
                .addSchemas("Problem", new Schema<>().type("object")
                        .required(List.of(STATUS_PROPERTY, "title"))
                        .addProperty(STATUS_PROPERTY, new IntegerSchema())
                        .addProperty("title", new StringSchema())
                        .addProperty("detail", new StringSchema())
                        .addProperty("type", new StringSchema())
                        .addProperty("instance", new StringSchema()));
        components.addResponses(BAD_REQUEST_RESPONSE, problem("Некорректный UUID, JSON или отсутствующая/null/неизвестная команда"));
        components.addResponses(NOT_FOUND_RESPONSE, problem("Партия не найдена"));
        components.addResponses("UnsupportedMediaType", problem("Ожидается application/json"));
        components.addResponses(OPERATION_FAILED_RESPONSE, problem("Сбой операции; внутренние детали доступны только в журнале"));
        return new OpenAPI().info(new Info().title("Tetris API").version("1.0.0")
                .description("Партии хранятся в памяти процесса. Доступ локального демо без аутентификации. "
                        + "Try it out выполняет реальные действия; перезапуск сервера удаляет партии."))
                .servers(List.of(new Server().url("/"))).components(components);
    }

    /**
     * Уточняет ограничения неизменяемых моделей и фактические ошибки операций.
     *
     * @return дополнение автоматически построенного контракта
     */
    @Bean
    public OpenApiCustomizer gameContract() {
        return document -> {
            var schemas = document.getComponents().getSchemas();
            for (String name : List.of("GameSnapshot", "ActiveView", "Cell", "CommandOutcome", "CommandBody")) {
                Schema<?> schema = schemas.get(name);
                schema.setRequired(List.copyOf(schema.getProperties().keySet()));
            }
            Schema<?> snapshot = schemas.get("GameSnapshot");
            for (String property : List.of("width", "height")) {
                snapshot.getProperties().get(property).setMinimum(BigDecimal.ONE);
            }
            snapshot.getProperties().get("clearedLines").setMinimum(BigDecimal.ZERO);
            var active = snapshot.getProperties().get(ACTIVE_PROPERTY);
            snapshot.addProperty(ACTIVE_PROPERTY, new ComposedSchema().addAllOfItem(active).nullable(true));
            Schema<?> view = schemas.get("ActiveView");
            view.getProperties().get("cells").minItems(4).maxItems(4).uniqueItems(true);
            Schema<?> cell = schemas.get("Cell");
            for (String coordinate : List.of("x", "y")) {
                cell.getProperties().get(coordinate).setMinimum(BigDecimal.ZERO);
            }
            snapshot.setExample(Map.of("gameId", "00000000-0000-0000-0000-000000000001",
                    "width", 10, "height", 20, STATUS_PROPERTY, "RUNNING", "settled", List.of(), "clearedLines", 0,
                    ACTIVE_PROPERTY, Map.of("kind", "O", "cells", List.of(
                            Map.of("x", 4, "y", 0), Map.of("x", 5, "y", 0),
                            Map.of("x", 4, "y", 1), Map.of("x", 5, "y", 1)))));
            document.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                switch (operation.getOperationId()) {
                    case "createGame" -> describeCreation(operation);
                    case "readGame" -> {
                        error(operation, "400", BAD_REQUEST_RESPONSE);
                        error(operation, "404", NOT_FOUND_RESPONSE);
                    }
                    case "executeCommand" -> {
                        operation.getRequestBody().setRequired(true);
                        error(operation, "400", BAD_REQUEST_RESPONSE);
                        error(operation, "404", NOT_FOUND_RESPONSE);
                        error(operation, "415", "UnsupportedMediaType");
                        error(operation, "500", OPERATION_FAILED_RESPONSE);
                    }
                    default -> throw new IllegalStateException("Неописанная операция: " + operation.getOperationId());
                }
            }));
        };
    }

    /**
     * Описывает реальный статус создания и адрес чтения новой партии.
     *
     * @param operation операция создания, выведенная из контроллера
     */
    private static void describeCreation(Operation operation) {
        operation.getResponses().remove("200");
        operation.getResponses().addApiResponse("201", new ApiResponse().description("Партия создана")
                .addHeaderObject("Location", new Header().description("Путь чтения созданной партии")
                        .schema(new StringSchema().example("/api/games/00000000-0000-0000-0000-000000000001")))
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/GameSnapshot")))));
        error(operation, "500", OPERATION_FAILED_RESPONSE);
    }

    /**
     * Создаёт общее описание тела ошибки без внутренних деталей исключения.
     *
     * @param description пояснение причины
     * @return ответ с application/problem+json
     */
    private static ApiResponse problem(String description) {
        return new ApiResponse().description(description).content(new Content()
                .addMediaType("application/problem+json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/Problem"))));
    }

    /**
     * Подключает общее описание ошибки к применимой HTTP-операции.
     *
     * @param operation документируемая операция
     * @param status код HTTP
     * @param response имя общего ответа
     */
    private static void error(Operation operation, String status, String response) {
        operation.getResponses().addApiResponse(status, new ApiResponse().$ref("#/components/responses/" + response));
    }
}
