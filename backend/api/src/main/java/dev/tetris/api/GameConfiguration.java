package dev.tetris.api;

import dev.tetris.core.TetrisGame;
import dev.tetris.core.piece.O;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Выбирает фабрику партий для обычного запуска и воспроизводимых сквозных тестов.
 */
@Configuration
public class GameConfiguration {
    /**
     * Создаёт конфигурацию выбора фабрики партий по профилю.
     */
    public GameConfiguration() {
        // Конфигурация не хранит состояние; зависимости создаются методами @Bean.
    }

    /**
     * Настраивает отдельную стандартную партию для каждого создания по HTTP.
     *
     * @return фабрика поля 10 × 20 со случайными фигурами
     */
    @Bean
    @Profile("!e2e")
    public Supplier<TetrisGame> gameFactory() {
        return TetrisGame::new;
    }

    /**
     * Изолирует предсказуемую геометрию в профиле e2e.
     *
     * @return фабрика поля 4 × 4 с повторяющейся фигурой O
     */
    @Bean
    @Profile("e2e")
    public Supplier<TetrisGame> scriptedFactory() {
        return () -> new TetrisGame(4, 4, O::new);
    }
}
