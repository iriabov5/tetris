package dev.tetris.core;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.*;
import static dev.tetris.core.CoreTestFixtures.moveDown;

/**
 * Расширяемость — подключение нового типа фигуры.
 */
@DisplayName("Расширяемость — подключение нового типа фигуры")
class TetrisGameExtensionTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    TetrisGameExtensionTest() {
    }

    /**
     * Выпускает, поворачивает и закрепляет дополнительный тип через обычные команды.
     */
    @Test
    @DisplayName("Выпускает, поворачивает и закрепляет дополнительный тип через обычные команды")
    void usesCustomTypeThroughNormalLifecycle() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, TestTetromino::new);

        // Act
        CommandOutcome rotation = game.execute(Command.ROTATE);
        moveDown(game, 3);

        // Assert
        assertEquals(CommandResult.APPLIED, rotation.result());
        assertEquals("CUSTOM", game.snapshot().active().kind());
        assertEquals(4, game.snapshot().settled().size());
    }

    /**
     * Сохраняет партию при недопустимом результате пользовательского поворота.
     *
     * @param scenario имя ошибочного результата
     * @param cells    результат поворота подключённого типа
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRotations")
    @DisplayName("Сохраняет партию при недопустимом результате пользовательского поворота")
    void rejectsInvalidRotationWithoutStateChange(String scenario, List<Cell> cells) {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, () -> new TestTetromino(cells));
        GameSnapshot before = game.snapshot();

        // Act
        assertThrows(IllegalArgumentException.class, () -> game.execute(Command.ROTATE));

        // Assert
        assertEquals(before, game.snapshot());
    }

    /**
     * Разделяет нарушения количества и уникальности клеток по именованным случаям.
     *
     * @return ошибочные результаты пользовательского поворота
     */
    static Stream<Arguments> invalidRotations() {
        return Stream.of(
                Arguments.of("пустой результат", List.<Cell>of()),
                Arguments.of("повторяющиеся клетки", List.of(
                        new Cell(0, 0), new Cell(0, 0), new Cell(1, 0), new Cell(1, 1))));
    }
}
