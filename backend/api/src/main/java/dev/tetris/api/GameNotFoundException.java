package dev.tetris.api;

/**
 * Сигнализирует, что UUID из запроса отсутствует в реестре партий.
 */
public final class GameNotFoundException extends RuntimeException {
    /**
     * Создаёт сигнал отсутствия UUID в реестре партий.
     */
    public GameNotFoundException() {
        // Исключение не хранит дополнительных данных; описание HTTP-ошибки формирует ApiErrors.
    }
}
