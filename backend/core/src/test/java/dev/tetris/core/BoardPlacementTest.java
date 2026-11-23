package dev.tetris.core;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Поле — допустимость размещения.
 */
@DisplayName("Поле — допустимость размещения")
class BoardPlacementTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    BoardPlacementTest() {
    }

    /**
     * Разрешает размещение на свободных клетках.
     */
    @Test
    @DisplayName("Разрешает размещение на свободных клетках")
    void allowsFreeCells() {
        // Arrange
        Board board = new Board(4, 6);
        List<Cell> square = List.of(new Cell(0, 4), new Cell(1, 4), new Cell(0, 5), new Cell(1, 5));

        // Act
        boolean allowed = board.canPlace(square);

        // Assert
        assertTrue(allowed);
    }

    /**
     * Отклоняет клетку за границей поля.
     *
     * @param x координата проверяемой клетки по горизонтали
     * @param y координата проверяемой клетки по вертикали
     */
    @ParameterizedTest(name = "x={0}, y={1}")
    @CsvSource({"-1, 0", "4, 0", "0, -1", "0, 6"})
    @DisplayName("Отклоняет клетку за границей поля")
    void rejectsEachBoundary(int x, int y) {
        // Arrange
        Board board = new Board(4, 6);

        // Act
        boolean allowed = board.canPlace(List.of(new Cell(x, y)));

        // Assert
        assertFalse(allowed);
    }

    /**
     * Отклоняет пересечение с закреплёнными клетками.
     */
    @Test
    @DisplayName("Отклоняет пересечение с закреплёнными клетками")
    void rejectsOccupiedCells() {
        // Arrange
        Board board = new Board(4, 6);
        List<Cell> square = List.of(new Cell(0, 4), new Cell(1, 4), new Cell(0, 5), new Cell(1, 5));
        board.lock(square);

        // Act
        boolean allowed = board.canPlace(square);

        // Assert
        assertFalse(allowed);
    }

    /**
     * Отклоняет неположительный размер поля.
     *
     * @param width  ширина поля
     * @param height высота поля
     */
    @ParameterizedTest(name = "поле {0} × {1}")
    @CsvSource({"0, 4", "4, -1"})
    @DisplayName("Отклоняет неположительный размер поля")
    void rejectsInvalidDimensions(int width, int height) {
        // Arrange
        int expectedInvalidWidth = width;
        int expectedInvalidHeight = height;

        // Act
        var failure = assertThrows(IllegalArgumentException.class,
                () -> new Board(expectedInvalidWidth, expectedInvalidHeight));

        // Assert
        assertEquals("Размеры поля должны быть положительными", failure.getMessage());
    }

}
