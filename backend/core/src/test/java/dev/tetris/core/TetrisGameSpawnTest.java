package dev.tetris.core;

import dev.tetris.core.piece.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Партия — начальное состояние и выпуск фигур.
 */
@DisplayName("Партия — начальное состояние и выпуск фигур")
class TetrisGameSpawnTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    TetrisGameSpawnTest() {
    }

    /**
     * Создаёт пустую работающую партию на поле 10 на 20.
     */
    @Test
    @DisplayName("Создаёт пустую работающую партию на поле 10 на 20")
    void startsDefaultGame() {
        // Arrange
        int expectedWidth = 10;
        int expectedHeight = 20;

        // Act
        GameSnapshot state = new TetrisGame().snapshot();

        // Assert
        assertEquals(expectedWidth, state.width());
        assertEquals(expectedHeight, state.height());
        assertEquals(GameStatus.RUNNING, state.status());
        assertEquals(0, state.clearedLines());
        assertTrue(state.settled().isEmpty());
        assertNotNull(state.active());
    }

    /**
     * Выпускает фигуру в верхней строке по центру.
     *
     * @param kind имя типа в отчёте тестов
     * @param type определение проверяемой фигуры
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("dev.tetris.core.CoreTestFixtures#standardTypes")
    @DisplayName("Выпускает фигуру в верхней строке по центру")
    void centersEachStandardType(String kind, Tetromino type) {
        // Arrange
        TetrisGame game = new TetrisGame(10, 20, () -> type);

        // Act
        GameSnapshot state = game.snapshot();

        // Assert
        List<Cell> cells = state.active().cells();
        assertEquals(kind, state.active().kind());
        assertEquals(4, Set.copyOf(cells).size());
        assertEquals(0, cells.stream().mapToInt(Cell::y).min().orElseThrow());
        int left = cells.stream().mapToInt(Cell::x).min().orElseThrow();
        int right = cells.stream().mapToInt(Cell::x).max().orElseThrow();
        assertEquals(Math.floorDiv(10 - (right - left + 1), 2), left);
    }

    /**
     * Завершает партию, если первая фигура не помещается.
     */
    @Test
    @DisplayName("Завершает партию, если первая фигура не помещается")
    void finishesWhenFirstPieceDoesNotFit() {
        // Arrange
        PieceSource source = O::new;

        // Act
        GameSnapshot state = new TetrisGame(1, 1, source).snapshot();

        // Assert
        assertEquals(GameStatus.GAME_OVER, state.status());
        assertNull(state.active());
    }

}
