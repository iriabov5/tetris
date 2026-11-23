package dev.tetris.core;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Поле — закрепление и удаление строк.
 */
@DisplayName("Поле — закрепление и удаление строк")
class BoardLockingTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    BoardLockingTest() {
    }

    /**
     * Закрепляет четыре клетки без удаления неполных строк.
     */
    @Test
    @DisplayName("Закрепляет четыре клетки без удаления неполных строк")
    void locksFourCells() {
        // Arrange
        Board board = new Board(4, 6);
        List<Cell> square = List.of(new Cell(0, 4), new Cell(1, 4), new Cell(0, 5), new Cell(1, 5));

        // Act
        int cleared = board.lock(square);

        // Assert
        assertEquals(0, cleared);
        assertEquals(Set.copyOf(square), Set.copyOf(board.cells()));
    }

    /**
     * Сохраняет поле при попытке повторного закрепления.
     */
    @Test
    @DisplayName("Сохраняет поле при попытке повторного закрепления")
    void rejectsOverlappingLockWithoutChanges() {
        // Arrange
        Board board = new Board(4, 6);
        List<Cell> square = List.of(new Cell(0, 4), new Cell(1, 4), new Cell(0, 5), new Cell(1, 5));
        board.lock(square);
        List<Cell> before = board.cells();

        // Act
        assertThrows(IllegalArgumentException.class, () -> board.lock(square));

        // Assert
        assertEquals(before, board.cells());
    }

    /**
     * Сохраняет поле при попытке закрепить пустую фигуру.
     */
    @Test
    @DisplayName("Сохраняет поле при попытке закрепить пустую фигуру")
    void rejectsInvalidCellCount() {
        // Arrange
        Board board = new Board(4, 6);

        // Act
        assertThrows(IllegalArgumentException.class, () -> board.lock(List.of()));

        // Assert
        assertTrue(board.cells().isEmpty());
    }

    /**
     * Сохраняет поле при повторяющихся клетках фигуры.
     */
    @Test
    @DisplayName("Сохраняет поле при повторяющихся клетках фигуры")
    void rejectsDuplicateCells() {
        // Arrange
        Board board = new Board(4, 6);
        List<Cell> duplicates = List.of(
                new Cell(2, 0), new Cell(2, 0), new Cell(3, 0), new Cell(3, 1));

        // Act
        assertThrows(IllegalArgumentException.class, () -> board.lock(duplicates));

        // Assert
        assertTrue(board.cells().isEmpty());
    }

    /**
     * Удаляет несмежные полные строки и опускает остальные клетки.
     */
    @Test
    @DisplayName("Удаляет несмежные полные строки и опускает остальные клетки")
    void clearsNonAdjacentRows() {
        // Arrange
        Board board = new Board(4, 6);
        board.lock(List.of(new Cell(0, 5), new Cell(1, 5), new Cell(0, 3), new Cell(1, 3)));
        board.lock(List.of(new Cell(0, 1), new Cell(1, 1), new Cell(2, 1), new Cell(0, 0)));
        List<Cell> completingCells = List.of(
                new Cell(2, 5), new Cell(3, 5), new Cell(2, 3), new Cell(3, 3));

        // Act
        int cleared = board.lock(completingCells);

        // Assert
        assertEquals(2, cleared);
        assertEquals(Set.of(new Cell(0, 3), new Cell(1, 3), new Cell(2, 3), new Cell(0, 2)),
                Set.copyOf(board.cells()));
    }

}
