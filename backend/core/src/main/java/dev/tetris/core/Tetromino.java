package dev.tetris.core;

import java.util.List;
import java.util.Objects;

/**
 * Общий контракт определения фигуры с неизменяемыми исходными клетками.
 * Наследники задают геометрию и при необходимости переопределяют поворот.
 * Координаты здесь локальные, выход кандидата за границы проверяет поле.
 */
public abstract class Tetromino {
    /**
     * Строковый идентификатор расширяемого типа.
     */
    private final String kind;
    /**
     * Независимая неизменяемая исходная геометрия.
     */
    private final List<Cell> initialCells;
    /**
     * Горизонтальный центр поворота.
     */
    private final double pivotX;
    /**
     * Вертикальный центр поворота.
     */
    private final double pivotY;

    /**
     * Сохраняет независимую исходную геометрию и центр вращения.
     *
     * @param kind   непустой идентификатор типа
     * @param cells  четыре разные исходные клетки
     * @param pivotX горизонтальная координата центра поворота
     * @param pivotY вертикальная координата центра поворота
     * @throws IllegalArgumentException если имя пустое или набор клеток недопустим
     * @throws NullPointerException     если имя, список или его клетка равны null
     */
    protected Tetromino(String kind, List<Cell> cells, double pivotX, double pivotY) {
        this.kind = Objects.requireNonNull(kind);
        this.initialCells = PieceGeometry.copyOf(cells);
        if (kind.isBlank()) {
            throw new IllegalArgumentException("Фигура должна иметь непустое имя");
        }
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    /**
     * Предоставляет идентификатор типа без раскрытия изменяемого состояния.
     *
     * @return имя фигуры, передаваемое в снимке партии
     */
    public final String kind() {
        return kind;
    }

    /**
     * Предоставляет геометрию начальной ориентации.
     *
     * @return неизменяемые локальные координаты четырёх клеток
     */
    public final List<Cell> initialCells() {
        return initialCells;
    }

    /**
     * Поворачивает локальные клетки на 90 градусов по часовой стрелке вокруг центра типа.
     * Наследник может задать другое поведение, сохраняя четыре разные клетки.
     *
     * @param cells текущие локальные координаты
     * @return неизменяемый список локальных клеток следующей ориентации
     */
    public List<Cell> rotate(List<Cell> cells) {
        return cells.stream().map(cell -> new Cell(
                (int) Math.round(pivotX - (cell.y() - pivotY)),
                (int) Math.round(pivotY + (cell.x() - pivotX)))).toList();
    }
}
