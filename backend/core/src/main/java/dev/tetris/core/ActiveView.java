package dev.tetris.core;

import java.util.List;

/**
 * Неизменяемое представление активной фигуры для потребителя снимка.
 *
 * @param kind  строковый идентификатор типа
 * @param cells четыре клетки в координатах поля
 */
public record ActiveView(String kind, List<Cell> cells) {
    /**
     * Защищает коллекцию клеток от изменений после выдачи снимка.
     *
     * @param kind  строковый идентификатор типа
     * @param cells четыре клетки в координатах поля
     */
    public ActiveView {
        cells = List.copyOf(cells);
    }
}
