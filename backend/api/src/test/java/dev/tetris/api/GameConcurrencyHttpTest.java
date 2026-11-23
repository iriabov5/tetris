package dev.tetris.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP — одновременные команды.
 */
@DisplayName("HTTP — одновременные команды")
class GameConcurrencyHttpTest extends GameHttpSupport {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    GameConcurrencyHttpTest() {
    }

    /**
     * Применяет два одновременных шага без потери обновления.
     *
     * @throws Exception при ошибке HTTP-запроса или ожидания конкурентной операции
     */
    @Test
    @DisplayName("Применяет два одновременных шага без потери обновления")
    void appliesBothConcurrentSteps() throws Exception {
        // Arrange
        GameApiClient api = client();
        String id = api.json(api.create()).get("gameId").asText();
        CyclicBarrier barrier = new CyclicBarrier(2);
        var step = (java.util.concurrent.Callable<JsonNode>) () -> {
            barrier.await(5, TimeUnit.SECONDS);
            return api.json(api.command(id, "DOWN")).get("state");
        };

        // Act
        JsonNode firstState;
        JsonNode secondState;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(step);
            var second = executor.submit(step);
            firstState = first.get(10, TimeUnit.SECONDS);
            secondState = second.get(10, TimeUnit.SECONDS);
        }

        // Assert
        assertEquals(Set.of(1, 2), Set.of(minimumY(firstState), minimumY(secondState)));
        assertEquals(2, minimumY(api.json(api.read(id))));
    }

    /**
     * Определяет строку верхней клетки активной фигуры для сравнения двух шагов.
     *
     * @param state снимок после HTTP-команды
     * @return минимальная координата y активных клеток
     */
    private int minimumY(JsonNode state) {
        int minimum = Integer.MAX_VALUE;
        for (JsonNode cell : state.at("/active/cells")) {
            minimum = Math.min(minimum, cell.get("y").asInt());
        }
        return minimum;
    }
}
