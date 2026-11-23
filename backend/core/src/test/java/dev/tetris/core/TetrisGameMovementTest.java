package dev.tetris.core;

import dev.tetris.core.piece.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Партия — перемещение активной фигуры.
 */
@DisplayName("Партия — перемещение активной фигуры")
class TetrisGameMovementTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    TetrisGameMovementTest() {
    }

    /**
     * Сдвигает фигуру на одну клетку в выбранном направлении.
     *
     * @param command направление сдвига
     * @param dx      ожидаемое смещение по горизонтали
     * @param dy      ожидаемое смещение по вертикали
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"LEFT, -1, 0", "RIGHT, 1, 0", "DOWN, 0, 1"})
    @DisplayName("Сдвигает фигуру на одну клетку в выбранном направлении")
    void movesOneCell(Command command, int dx, int dy) {
        // Arrange
        TetrisGame game = new TetrisGame(10, 20, O::new);
        List<Cell> before = game.snapshot().active().cells();

        // Act
        CommandOutcome outcome = game.execute(command);

        // Assert
        assertEquals(CommandResult.APPLIED, outcome.result());
        assertEquals(before.stream().map(cell -> cell.translated(dx, dy)).toList(),
                outcome.state().active().cells());
        assertTrue(outcome.state().settled().isEmpty());
    }

    /**
     * Сохраняет состояние при заблокированном сдвиге у стены.
     *
     * @param direction направление к проверяемой стене
     */
    @ParameterizedTest(name = "стена в направлении {0}")
    @CsvSource({"LEFT", "RIGHT"})
    @DisplayName("Сохраняет состояние при заблокированном сдвиге у стены")
    void preservesStateAtWall(Command direction) {
        // Arrange
        TetrisGame game = new TetrisGame(10, 20, I::new);
        for (int step = 0; step < 10; step++) {
            game.execute(direction);
        }
        GameSnapshot before = game.snapshot();

        // Act
        CommandOutcome outcome = game.execute(direction);

        // Assert
        assertEquals(CommandResult.BLOCKED, outcome.result());
        assertEquals(before, outcome.state());
    }

    /**
     * Отклоняет null-команду без изменения состояния.
     */
    @Test
    @DisplayName("Отклоняет null-команду без изменения состояния")
    void rejectsNullCommand() {
        // Arrange
        TetrisGame game = new TetrisGame();
        GameSnapshot before = game.snapshot();

        // Act
        assertThrows(NullPointerException.class, () -> game.execute(null));

        // Assert
        assertEquals(before, game.snapshot());
    }

}
