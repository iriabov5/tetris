package dev.tetris.core;

import dev.tetris.core.piece.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static dev.tetris.core.CoreTestFixtures.moveDown;

/**
 * Партия — закрепление, очистка строк и окончание.
 */
@DisplayName("Партия — закрепление, очистка строк и окончание")
class TetrisGameLifecycleTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    TetrisGameLifecycleTest() {
    }

    /**
     * Закрепляет фигуру у дна и выпускает следующую.
     */
    @Test
    @DisplayName("Закрепляет фигуру у дна и выпускает следующую")
    void locksAtFloor() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        moveDown(game, 2);

        // Act
        CommandOutcome outcome = game.execute(Command.DOWN);

        // Assert
        assertEquals(CommandResult.APPLIED, outcome.result());
        assertEquals(4, outcome.state().settled().size());
        assertEquals(0, outcome.state().active().cells().stream().mapToInt(Cell::y).min().orElseThrow());
        assertEquals(0, outcome.state().clearedLines());
    }

    /**
     * Удаляет две строки до выпуска следующей фигуры.
     */
    @Test
    @DisplayName("Удаляет две строки до выпуска следующей фигуры")
    void clearsTwoRowsBeforeSpawn() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        game.execute(Command.LEFT);
        moveDown(game, 3);
        game.execute(Command.RIGHT);
        moveDown(game, 2);

        // Act
        CommandOutcome outcome = game.execute(Command.DOWN);

        // Assert
        assertEquals(2, outcome.state().clearedLines());
        assertTrue(outcome.state().settled().isEmpty());
        assertEquals(GameStatus.RUNNING, outcome.state().status());
        assertEquals(4, outcome.state().active().cells().size());
    }

    /**
     * Завершает партию, когда после закрепления на клетках нет места для новой фигуры.
     */
    @Test
    @DisplayName("Завершает партию, когда после закрепления на клетках нет места для новой фигуры")
    void finishesAfterLockingOnCells() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        moveDown(game, 3);

        // Act
        CommandOutcome outcome = game.execute(Command.DOWN);

        // Assert
        assertEquals(CommandResult.APPLIED, outcome.result());
        assertEquals(GameStatus.GAME_OVER, outcome.state().status());
        assertNull(outcome.state().active());
        assertEquals(8, outcome.state().settled().size());
    }

    /**
     * Возвращает FINISHED и сохраняет завершённую партию.
     */
    @Test
    @DisplayName("Возвращает FINISHED и сохраняет завершённую партию")
    void ignoresCommandAfterFinish() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        moveDown(game, 4);
        GameSnapshot before = game.snapshot();

        // Act
        CommandOutcome outcome = game.execute(Command.DOWN);

        // Assert
        assertEquals(CommandResult.FINISHED, outcome.result());
        assertEquals(before, outcome.state());
    }

}
