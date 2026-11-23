package dev.tetris.core;

import java.util.List;
import java.util.UUID;

/**
 * Согласованное неизменяемое состояние партии, безопасное для передачи по HTTP.
 *
 * @param gameId       идентификатор партии
 * @param width        ширина поля
 * @param height       высота поля
 * @param status       состояние партии
 * @param settled      координаты закреплённых клеток
 * @param active       активная фигура или null при GAME_OVER
 * @param clearedLines общее число удалённых строк
 */
public record GameSnapshot(UUID gameId, int width, int height, GameStatus status,
                           List<Cell> settled, ActiveView active, int clearedLines) {
    /**
     * Копирует список закреплённых клеток для независимости выданных снимков.
     *
     * @param gameId       идентификатор партии
     * @param width        ширина поля
     * @param height       высота поля
     * @param status       состояние партии
     * @param settled      координаты закреплённых клеток
     * @param active       активная фигура или null при GAME_OVER
     * @param clearedLines общее число удалённых строк
     */
    public GameSnapshot {
        settled = List.copyOf(settled);
    }
}
