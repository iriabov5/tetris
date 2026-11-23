package dev.tetris.core;

import dev.tetris.core.piece.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static dev.tetris.core.CoreTestFixtures.moveDown;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Геометрия поворота — ожидаемые клетки каждой ориентации всех стандартных фигур.
 */
@DisplayName("Геометрия поворота — все фигуры и ориентации")
class TetrisGameRotationGeometryTest {
    /**
     * Создаёт независимую группу геометрических проверок поворота.
     */
    TetrisGameRotationGeometryTest() {
    }

    /**
     * Сравнивает результат команды с вручную заданными клетками нужной ориентации.
     *
     * @param kind имя фигуры в отчёте
     * @param turns число поворотов по часовой стрелке
     * @param type проверяемое определение фигуры
     * @param expected ожидаемые клетки на поле после заданного числа поворотов
     */
    @ParameterizedTest(name = "{0}: поворотов — {1}")
    @MethodSource("expectedOrientations")
    @DisplayName("Каждая ориентация совпадает с ожидаемыми координатами")
    void matchesExpectedOrientation(String kind, int turns, Tetromino type, Set<Cell> expected) {
        // Arrange
        TetrisGame game = new TetrisGame(10, 20, () -> type);
        moveDown(game, 5);

        // Act
        List<CommandOutcome> outcomes = IntStream.range(0, turns)
                .mapToObj(index -> game.execute(Command.ROTATE)).toList();

        // Assert
        assertTrue(outcomes.stream().allMatch(outcome -> outcome.result() == CommandResult.APPLIED));
        assertEquals(kind, game.snapshot().active().kind());
        assertEquals(expected, Set.copyOf(game.snapshot().active().cells()));
    }

    /**
     * Убеждается, что поворот не переносит фигуру за левую стену и не меняет квадрат O.
     *
     * @param kind имя фигуры в отчёте
     * @param type проверяемое определение фигуры
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("dev.tetris.core.CoreTestFixtures#standardTypes")
    @DisplayName("У левой стены поворот сохраняет клетки в пределах поля")
    void preservesPlacementAtLeftWall(String kind, Tetromino type) {
        // Arrange
        TetrisGame game = new TetrisGame(10, 20, () -> type);
        moveDown(game, 5);
        game.execute(Command.ROTATE);
        for (int step = 0; step < 10; step++) {
            game.execute(Command.LEFT);
        }
        GameSnapshot before = game.snapshot();
        CommandResult expectedResult = kind.equals("O") ? CommandResult.APPLIED : CommandResult.BLOCKED;

        // Act
        CommandOutcome outcome = game.execute(Command.ROTATE);

        // Assert
        assertEquals(expectedResult, outcome.result());
        assertEquals(before, outcome.state());
    }

    /**
     * Задаёт вручную четыре ориентации; координаты не вычисляются алгоритмом поворота из ядра.
     *
     * @return именованные случаи всех семи фигур и четырёх поворотов
     */
    static Stream<Arguments> expectedOrientations() {
        List<Cell> square = cells(0, 0, 1, 0, 0, 1, 1, 1);
        return Stream.of(
                cases(new I(), 3, 4, List.of(
                        cells(2, 0, 2, 1, 2, 2, 2, 3),
                        cells(0, 2, 1, 2, 2, 2, 3, 2),
                        cells(1, 0, 1, 1, 1, 2, 1, 3),
                        cells(0, 1, 1, 1, 2, 1, 3, 1))),
                cases(new O(), 4, 5, List.of(square, square, square, square)),
                cases(new T(), 3, 5, List.of(
                        cells(2, 1, 1, 0, 1, 1, 1, 2),
                        cells(1, 2, 2, 1, 1, 1, 0, 1),
                        cells(0, 1, 1, 2, 1, 1, 1, 0),
                        cells(1, 0, 0, 1, 1, 1, 2, 1))),
                cases(new S(), 3, 5, List.of(
                        cells(2, 1, 2, 2, 1, 0, 1, 1),
                        cells(1, 2, 0, 2, 2, 1, 1, 1),
                        cells(0, 1, 0, 0, 1, 2, 1, 1),
                        cells(1, 0, 2, 0, 0, 1, 1, 1))),
                cases(new Z(), 3, 5, List.of(
                        cells(2, 0, 2, 1, 1, 1, 1, 2),
                        cells(2, 2, 1, 2, 1, 1, 0, 1),
                        cells(0, 2, 0, 1, 1, 1, 1, 0),
                        cells(0, 0, 1, 0, 1, 1, 2, 1))),
                cases(new J(), 3, 5, List.of(
                        cells(2, 0, 1, 0, 1, 1, 1, 2),
                        cells(2, 2, 2, 1, 1, 1, 0, 1),
                        cells(0, 2, 1, 2, 1, 1, 1, 0),
                        cells(0, 0, 0, 1, 1, 1, 2, 1))),
                cases(new L(), 3, 5, List.of(
                        cells(2, 2, 1, 0, 1, 1, 1, 2),
                        cells(0, 2, 2, 1, 1, 1, 0, 1),
                        cells(0, 0, 1, 2, 1, 1, 1, 0),
                        cells(2, 0, 0, 1, 1, 1, 2, 1))))
                .flatMap(stream -> stream);
    }

    /**
     * Переносит явно заданные локальные ожидания в известную стартовую позицию на поле.
     *
     * @param type проверяемый тип
     * @param x горизонтальное смещение шаблона после выпуска на поле шириной 10
     * @param y вертикальное смещение шаблона после пяти шагов вниз
     * @param orientations четыре вручную заданные ориентации
     * @return случаи поворотов от одного до четырёх
     */
    private static Stream<Arguments> cases(Tetromino type, int x, int y, List<List<Cell>> orientations) {
        return IntStream.range(0, orientations.size()).mapToObj(index -> {
            Set<Cell> expected = orientations.get(index).stream()
                    .map(cell -> new Cell(cell.x() + x, cell.y() + y))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            return Arguments.of(type.kind(), index + 1, type, expected);
        });
    }

    /**
     * Записывает ожидаемую геометрию компактными парами x, y.
     *
     * @param coordinates последовательность пар координат клеток
     * @return неизменяемый список заданных клеток
     */
    private static List<Cell> cells(int... coordinates) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < coordinates.length; index += 2) {
            cells.add(new Cell(coordinates[index], coordinates[index + 1]));
        }
        return List.copyOf(cells);
    }
}
