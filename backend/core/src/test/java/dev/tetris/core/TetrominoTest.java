package dev.tetris.core;

import dev.tetris.core.piece.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Определение фигуры — инварианты и неизменяемость.
 */
@DisplayName("Определение фигуры — инварианты и неизменяемость")
class TetrominoTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    TetrominoTest() {
    }

    /**
     * Запрещает изменение исходных клеток фигуры.
     */
    @Test
    @DisplayName("Запрещает изменение исходных клеток фигуры")
    void protectsInitialGeometry() {
        // Arrange
        Tetromino type = new O();
        List<Cell> before = type.initialCells();

        // Act
        assertThrows(UnsupportedOperationException.class, before::clear);

        // Assert
        assertEquals(before, type.initialCells());
    }

    /**
     * Отклоняет пустое имя типа фигуры.
     */
    @Test
    @DisplayName("Отклоняет пустое имя типа фигуры")
    void rejectsBlankKind() {
        // Arrange
        List<Cell> cells = new O().initialCells();

        // Act
        var failure = assertThrows(IllegalArgumentException.class,
                () -> new Tetromino("", cells, 0, 0) {
                });

        // Assert
        assertEquals("Фигура должна иметь непустое имя", failure.getMessage());
    }

    /**
     * Отклоняет фигуру с числом клеток меньше четырёх.
     */
    @Test
    @DisplayName("Отклоняет фигуру с числом клеток меньше четырёх")
    void rejectsWrongCellCount() {
        // Arrange
        List<Cell> cells = List.of(new Cell(0, 0));

        // Act
        var failure = assertThrows(IllegalArgumentException.class,
                () -> new Tetromino("CUSTOM", cells, 0, 0) {
                });

        // Assert
        assertEquals("Фигура должна содержать четыре разные клетки", failure.getMessage());
    }

    /**
     * Отклоняет фигуру с повторяющимися клетками.
     */
    @Test
    @DisplayName("Отклоняет фигуру с повторяющимися клетками")
    void rejectsDuplicateCells() {
        // Arrange
        List<Cell> cells = List.of(new Cell(0, 0), new Cell(0, 0), new Cell(1, 0), new Cell(1, 1));

        // Act
        var failure = assertThrows(IllegalArgumentException.class,
                () -> new Tetromino("CUSTOM", cells, 0, 0) {
                });

        // Assert
        assertEquals("Фигура должна содержать четыре разные клетки", failure.getMessage());
    }

}
