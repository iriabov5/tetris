package dev.tetris.core.piece;

import dev.tetris.core.Cell;
import dev.tetris.core.Tetromino;
import java.util.List;

/**
 * Определение фигуры O с четырьмя локальными клетками и собственным центром поворота.
 */
public final class O extends Tetromino {
    /**
     * Задаёт начальную ориентацию фигуры O и центр её вращения.
     */
    public O() {
        super("O", List.of(new Cell(0, 0), new Cell(1, 0), new Cell(0, 1), new Cell(1, 1)), 0.5, 0.5);
    }

    /**
     * Сохраняет геометрию квадрата при команде поворота.
     * @param cells текущие локальные клетки
     * @return независимый неизменяемый список тех же координат
     */
    @Override
    public List<Cell> rotate(List<Cell> cells) {
        return List.copyOf(cells);
    }
}
