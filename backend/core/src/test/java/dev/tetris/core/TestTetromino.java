package dev.tetris.core;

import dev.tetris.core.piece.O;

import java.util.List;

/**
 * Дополнительный тип, подтверждающий расширение партии через общий контракт.
 */
final class TestTetromino extends Tetromino {
    /**
     * Геометрия, которую вернёт пользовательский поворот.
     */
    private final List<Cell> rotation;

    /**
     * Создаёт допустимый дополнительный тип с геометрией квадрата.
     */
    TestTetromino() {
        this(new O().initialCells());
    }

    /**
     * Задаёт результат поворота для проверки защиты от ошибочного расширения.
     *
     * @param rotation клетки, возвращаемые поворотом
     */
    TestTetromino(List<Cell> rotation) {
        super("CUSTOM", new O().initialCells(), 0.5, 0.5);
        this.rotation = List.copyOf(rotation);
    }

    /**
     * Возвращает заданную геометрию для проверки контракта подключения фигур.
     *
     * @param cells текущие локальные клетки
     * @return независимый неизменяемый результат пользовательского поворота
     */
    @Override
    public List<Cell> rotate(List<Cell> cells) {
        return rotation;
    }
}
