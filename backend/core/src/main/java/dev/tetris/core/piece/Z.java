package dev.tetris.core.piece;

import dev.tetris.core.Cell;
import dev.tetris.core.Tetromino;
import java.util.List;

/**
 * Определение фигуры Z с четырьмя локальными клетками и собственным центром поворота.
 */
public final class Z extends Tetromino {
    /**
     * Задаёт начальную ориентацию фигуры Z и центр её вращения.
     */
    public Z() {
        super("Z", List.of(new Cell(0, 0), new Cell(1, 0), new Cell(1, 1), new Cell(2, 1)), 1, 1);
    }
}
