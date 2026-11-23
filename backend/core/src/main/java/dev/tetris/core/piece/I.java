package dev.tetris.core.piece;

import dev.tetris.core.Cell;
import dev.tetris.core.Tetromino;
import java.util.List;

/**
 * Определение фигуры I с четырьмя локальными клетками и собственным центром поворота.
 */
public final class I extends Tetromino {
    /**
     * Задаёт начальную ориентацию фигуры I и центр её вращения.
     */
    public I() {
        super("I", List.of(new Cell(0, 1), new Cell(1, 1), new Cell(2, 1), new Cell(3, 1)), 1.5, 1.5);
    }
}
