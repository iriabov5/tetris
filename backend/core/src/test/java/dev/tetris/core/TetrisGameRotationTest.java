package dev.tetris.core;

import dev.tetris.core.piece.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static dev.tetris.core.CoreTestFixtures.moveDown;

/**
 * Партия — повороты фигур.
 */
@DisplayName("Партия — повороты фигур")
class TetrisGameRotationTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    TetrisGameRotationTest() {
    }

    /**
     * Поворачивает I в вертикальное положение.
     */
    @Test
    @DisplayName("Поворачивает I в вертикальное положение")
    void rotatesIVertically() {
        // Arrange
        TetrisGame game = new TetrisGame(10, 20, I::new);
        moveDown(game, 3);

        // Act
        CommandOutcome outcome = game.execute(Command.ROTATE);

        // Assert
        assertEquals(CommandResult.APPLIED, outcome.result());
        assertEquals(1, outcome.state().active().cells().stream().map(Cell::x).distinct().count());
    }

    /**
     * Сохраняет клетки O после поворота.
     */
    @Test
    @DisplayName("Сохраняет клетки O после поворота")
    void keepsOCells() {
        // Arrange
        TetrisGame game = new TetrisGame(10, 20, O::new);
        GameSnapshot before = game.snapshot();

        // Act
        CommandOutcome outcome = game.execute(Command.ROTATE);

        // Assert
        assertEquals(CommandResult.APPLIED, outcome.result());
        assertEquals(before, outcome.state());
    }

    /**
     * Отклоняет поворот I за верхнюю границу без изменения состояния.
     */
    @Test
    @DisplayName("Отклоняет поворот I за верхнюю границу без изменения состояния")
    void blocksRotationAboveTop() {
        // Arrange
        TetrisGame game = new TetrisGame(10, 20, I::new);
        GameSnapshot before = game.snapshot();

        // Act
        CommandOutcome outcome = game.execute(Command.ROTATE);

        // Assert
        assertEquals(CommandResult.BLOCKED, outcome.result());
        assertEquals(before, outcome.state());
    }

    /**
     * Отклоняет поворот в закреплённые клетки без изменения состояния.
     */
    @Test
    @DisplayName("Отклоняет поворот в закреплённые клетки без изменения состояния")
    void blocksRotationIntoSettledCells() {
        // Arrange
        AtomicInteger index = new AtomicInteger();
        TetrisGame game = new TetrisGame(6, 6, () -> index.getAndIncrement() == 0 ? new O() : new I());
        moveDown(game, 5);
        moveDown(game, 3);
        GameSnapshot before = game.snapshot();

        // Act
        CommandOutcome outcome = game.execute(Command.ROTATE);

        // Assert
        assertEquals(CommandResult.BLOCKED, outcome.result());
        assertEquals(before, outcome.state());
    }

}
