package dev.tetris.api;

import dev.tetris.core.TetrisGame;
import dev.tetris.core.piece.O;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP — единый ответ на неожиданный сбой операции без раскрытия внутренних деталей.
 */
@DisplayName("HTTP — неожиданный сбой операции")
@Import(GameOperationFailureHttpTest.FailingSourceConfiguration.class)
class GameOperationFailureHttpTest extends GameHttpSupport {
    /**
     * Создаёт независимую группу HTTP-проверок сбоя источника.
     */
    GameOperationFailureHttpTest() {
    }

    /**
     * Проверяет стандартный HTTP 500 и сохранение партии при ошибке сброса.
     *
     * @throws Exception при ошибке HTTP-запроса или разбора JSON
     */
    @Test
    @DisplayName("Сбой HARD_DROP возвращает problem+json 500 и сохраняет состояние")
    void returnsProblemWithoutChangingState() throws Exception {
        // Arrange
        GameApiClient api = client();
        var before = api.json(api.create());
        String id = before.get("gameId").asText();

        // Act
        var response = api.command(id, "HARD_DROP");

        // Assert
        api.assertProblem(500, response);
        assertFalse(response.body().contains("Внутренние детали источника"));
        assertEquals(before, api.json(api.read(id)));
    }

    /**
     * Изолирует отказ источника внутри текущего тестового контекста.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class FailingSourceConfiguration {
        /**
         * Создаёт конфигурацию фабрики с однократной ошибкой.
         */
        FailingSourceConfiguration() {
        }

        /**
         * Создаёт игру с источником, который отказывает на второй выдаче.
         *
         * @return фабрика независимых партий с контролируемым сбоем
         */
        @Bean
        @Primary
        Supplier<TetrisGame> failingGameFactory() {
            return () -> {
                AtomicInteger calls = new AtomicInteger();
                return new TetrisGame(4, 4, () -> {
                    if (calls.incrementAndGet() == 2) throw new IllegalStateException("Внутренние детали источника");
                    return new O();
                });
            };
        }
    }
}
