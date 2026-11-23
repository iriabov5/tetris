package dev.tetris.core;

import java.util.List;

/**
 * Единая проверка геометрии тетромино на границах определения, размещения и поля.
 */
final class PieceGeometry {
    /**
     * Исключает создание экземпляров служебного класса.
     */
    private PieceGeometry() {
        // Проверка не хранит состояние и вызывается статически.
    }

    /**
     * Копирует геометрию и проверяет инвариант четырёх разных клеток на копии.
     *
     * @param cells исходные координаты фигуры
     * @return независимый неизменяемый список четырёх разных клеток
     * @throws IllegalArgumentException если клеток не четыре или они повторяются
     * @throws NullPointerException если список или одна из клеток равны null
     */
    static List<Cell> copyOf(List<Cell> cells) {
        List<Cell> copy = List.copyOf(cells);
        if (copy.size() != 4 || copy.stream().distinct().count() != 4) {
            throw new IllegalArgumentException("Фигура должна содержать четыре разные клетки");
        }
        return copy;
    }
}
