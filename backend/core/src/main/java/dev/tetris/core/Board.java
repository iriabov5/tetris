package dev.tetris.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Хранит закреплённые клетки и общую проверку размещения в пределах сетки.
 * Доступен внутри ядра, сетка не выдаётся вызывающему коду.
 */
final class Board {
    /**
     * Ширина сетки.
     */
    private final int width;
    /**
     * Высота сетки.
     */
    private final int height;
    /**
     * Приватная сетка занятости, заменяемая после уплотнения строк.
     */
    private boolean[][] occupied;

    /**
     * Создаёт пустую прямоугольную сетку.
     *
     * @param width  положительное число столбцов
     * @param height положительное число строк
     * @throws IllegalArgumentException если любой размер неположительный
     */
    Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Размеры поля должны быть положительными");
        }
        this.width = width;
        this.height = height;
        occupied = new boolean[height][width];
    }

    /**
     * Проверяет все клетки кандидата относительно границ и закреплённой геометрии.
     *
     * @param cells координаты кандидата в системе поля
     * @return true, если каждая клетка внутри поля и свободна
     */
    boolean canPlace(List<Cell> cells) {
        return cells.stream().allMatch(cell -> cell.x() >= 0 && cell.x() < width
                && cell.y() >= 0 && cell.y() < height && !occupied[cell.y()][cell.x()]);
    }

    /**
     * Проверяет и закрепляет четыре клетки, затем одновременно удаляет полные строки.
     * Неполные строки переносятся снизу вверх с сохранением порядка.
     *
     * @param cells четыре разные координаты в системе поля
     * @return число удалённых строк
     * @throws IllegalArgumentException если клеток не четыре, они повторяются, пересекаются или находятся вне поля
     * @throws NullPointerException если список или одна из клеток равны null
     */
    int lock(List<Cell> cells) {
        List<Cell> validated = PieceGeometry.copyOf(cells);
        if (!canPlace(validated)) {
            throw new IllegalArgumentException("Недопустимое закрепление фигуры");
        }
        validated.forEach(cell -> occupied[cell.y()][cell.x()] = true);
        boolean[][] compacted = new boolean[height][width];
        int destination = height - 1;
        for (int row = height - 1; row >= 0; row--) {
            int count = 0;
            for (boolean cell : occupied[row]) {
                if (cell) {
                    count++;
                }
            }
            if (count < width) {
                compacted[destination--] = Arrays.copyOf(occupied[row], width);
            }
        }
        occupied = compacted;
        return destination + 1;
    }

    /**
     * Собирает независимое представление занятых клеток для снимка партии.
     *
     * @return неизменяемый список координат закреплённых клеток
     */
    List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (occupied[y][x]) {
                    cells.add(new Cell(x, y));
                }
            }
        }
        return List.copyOf(cells);
    }
}
