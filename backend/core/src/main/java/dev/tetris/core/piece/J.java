package dev.tetris.core.piece;

import dev.tetris.core.Cell;
import dev.tetris.core.Tetromino;
import java.util.List;

/**
 * Определение фигуры J с четырьмя локальными клетками и собственным центром поворота.
 */
public final class J extends Tetromino {
    /**
     * Задаёт начальную ориентацию фигуры J и центр её вращения.
     */
    public J() {
        super("J", List.of(new Cell(0, 0), new Cell(0, 1), new Cell(1, 1), new Cell(2, 1)), 1, 1);
    }
}
