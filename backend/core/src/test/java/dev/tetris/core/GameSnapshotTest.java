package dev.tetris.core;

import dev.tetris.core.piece.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static dev.tetris.core.CoreTestFixtures.moveDown;

/**
 * Снимок — защита данных партии.
 */
@DisplayName("Снимок — защита данных партии")
class GameSnapshotTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    GameSnapshotTest() {
    }

    /**
     * Запрещает изменение активных клеток через снимок.
     */
    @Test
    @DisplayName("Запрещает изменение активных клеток через снимок")
    void protectsActiveCells() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        GameSnapshot state = game.snapshot();
        var cells = state.active().cells();

        // Act
        assertThrows(UnsupportedOperationException.class, cells::clear);

        // Assert
        assertEquals(state, game.snapshot());
    }

    /**
     * Запрещает изменение закреплённых клеток через снимок.
     */
    @Test
    @DisplayName("Запрещает изменение закреплённых клеток через снимок")
    void protectsSettledCells() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        moveDown(game, 3);
        GameSnapshot state = game.snapshot();
        var cells = state.settled();

        // Act
        assertThrows(UnsupportedOperationException.class, cells::clear);

        // Assert
        assertEquals(state, game.snapshot());
    }

    /**
     * Сохраняет ранее выданный снимок после нового хода.
     */
    @Test
    @DisplayName("Сохраняет ранее выданный снимок после нового хода")
    void keepsOldSnapshot() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        GameSnapshot before = game.snapshot();

        // Act
        game.execute(Command.DOWN);

        // Assert
        assertEquals(0, before.active().cells().stream().mapToInt(Cell::y).min().orElseThrow());
        assertNotEquals(before, game.snapshot());
    }

}
