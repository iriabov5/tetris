package dev.tetris.core.piece;

import dev.tetris.core.Cell;
import dev.tetris.core.Tetromino;
import java.util.List;

/**
 * Определение фигуры T с четырьмя локальными клетками и собственным центром поворота.
 */
public final class T extends Tetromino {
    /**
     * Задаёт начальную ориентацию фигуры T и центр её вращения.
     */
    public T() {
        super("T", List.of(new Cell(1, 0), new Cell(0, 1), new Cell(1, 1), new Cell(2, 1)), 1, 1);
    }
}
