package dev.tetris.core.piece;

import dev.tetris.core.Cell;
import dev.tetris.core.Tetromino;
import java.util.List;

/**
 * Определение фигуры S с четырьмя локальными клетками и собственным центром поворота.
 */
public final class S extends Tetromino {
    /**
     * Задаёт начальную ориентацию фигуры S и центр её вращения.
     */
    public S() {
        super("S", List.of(new Cell(1, 0), new Cell(2, 0), new Cell(0, 1), new Cell(1, 1)), 1, 1);
    }
}
