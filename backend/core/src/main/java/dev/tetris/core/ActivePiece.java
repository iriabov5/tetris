package dev.tetris.core;

import java.util.List;

/**
 * Неизменяемое размещение фигуры, используемое для проверки кандидатов хода.
 *
 * @param type       определение типа фигуры
 * @param localCells четыре разные локальные клетки текущей ориентации
 * @param x          горизонтальное положение опорной точки
 * @param y          вертикальное положение опорной точки
 */
record ActivePiece(Tetromino type, List<Cell> localCells, int x, int y) {
    /**
     * Копирует клетки кандидата перед использованием в партии.
     *
     * @param type       определение типа фигуры
     * @param localCells четыре разные локальные клетки текущей ориентации
     * @param x          горизонтальное положение опорной точки
     * @param y          вертикальное положение опорной точки
     * @throws IllegalArgumentException если клеток не четыре или они повторяются
     * @throws NullPointerException если список или одна из клеток равны null
     */
    ActivePiece {
        localCells = PieceGeometry.copyOf(localCells);
    }

    /**
     * Центрирует занятые клетки и помещает их верхнюю строку на y = 0.
     * Опорная точка может находиться выше поля, если шаблон содержит пустую строку.
     *
     * @param type  выпускаемый тип
     * @param width ширина поля
     * @return кандидат стартового размещения
     */
    static ActivePiece spawn(Tetromino type, int width) {
        List<Cell> cells = type.initialCells();
        int minX = cells.stream().mapToInt(Cell::x).min().orElseThrow();
        int maxX = cells.stream().mapToInt(Cell::x).max().orElseThrow();
        int minY = cells.stream().mapToInt(Cell::y).min().orElseThrow();
        return new ActivePiece(type, cells, Math.floorDiv(width - (maxX - minX + 1), 2) - minX, -minY);
    }

    /**
     * Переводит локальную геометрию в систему координат поля.
     *
     * @return неизменяемые абсолютные координаты кандидата
     */
    List<Cell> cells() {
        return localCells.stream().map(cell -> cell.translated(x, y)).toList();
    }

    /**
     * Создаёт кандидат сдвига без изменения текущего размещения.
     *
     * @param dx горизонтальное смещение
     * @param dy вертикальное смещение
     * @return новое размещение с прежней ориентацией
     */
    ActivePiece translated(int dx, int dy) {
        return new ActivePiece(type, localCells, x + dx, y + dy);
    }

    /**
     * Передаёт расчёт ориентации определению типа, сохраняя опорную точку.
     *
     * @return кандидат поворота для последующей проверки полем
     */
    ActivePiece rotated() {
        return new ActivePiece(type, type.rotate(localCells), x, y);
    }
}
