package dev.tetris.core;

import dev.tetris.core.piece.O;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.tetris.core.CoreTestFixtures.moveDown;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Ошибки источника — сохранение партии и повтор команды после восстановления.
 */
@DisplayName("Ошибки источника — сохранение партии и восстановление")
class TetrisGameSourceFailureTest {
    /**
     * Создаёт независимую группу проверок ошибок источника.
     */
    TetrisGameSourceFailureTest() {
    }

    /**
     * Сохраняет активную фигуру, поле, статус и счётчик при сбое перед закреплением.
     */
    @Test
    @DisplayName("Сохраняет всю партию при ошибке источника перед закреплением")
    void preservesStateWhenSourceFails() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, failingOnceAt(2));
        moveDown(game, 2);
        GameSnapshot before = game.snapshot();

        // Act
        var failure = assertThrows(IllegalStateException.class, () -> game.execute(Command.DOWN));

        // Assert
        assertEquals("Источник временно недоступен", failure.getMessage());
        assertEquals(before, game.snapshot());
    }

    /**
     * Не удаляет полные строки и не меняет счётчик, если следующая фигура не получена.
     */
    @Test
    @DisplayName("Не изменяет клетки и счётчик перед очисткой строк при сбое источника")
    void preservesRowsWhenSourceFailsBeforeClearing() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, failingOnceAt(3));
        game.execute(Command.LEFT);
        moveDown(game, 3);
        game.execute(Command.RIGHT);
        moveDown(game, 2);
        GameSnapshot before = game.snapshot();

        // Act
        assertThrows(IllegalStateException.class, () -> game.execute(Command.DOWN));

        // Assert
        assertEquals(4, before.settled().size());
        assertEquals(0, before.clearedLines());
        assertEquals(before, game.snapshot());
    }

    /**
     * Позволяет повторить прерванный шаг после восстановления источника.
     */
    @Test
    @DisplayName("Повторяет закрепление после восстановления источника без пересечений")
    void retriesAfterSourceRecovers() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, failingOnceAt(2));
        moveDown(game, 2);
        assertThrows(IllegalStateException.class, () -> game.execute(Command.DOWN));

        // Act
        CommandOutcome outcome = game.execute(Command.DOWN);

        // Assert
        GameSnapshot state = outcome.state();
        assertEquals(CommandResult.APPLIED, outcome.result());
        assertEquals(GameStatus.RUNNING, state.status());
        assertEquals(4, state.settled().size());
        assertEquals(4, state.active().cells().size());
        assertTrue(state.active().cells().stream().noneMatch(state.settled()::contains));
    }

    /**
     * Защищает поле и активную фигуру от источника, вернувшего null вместо определения.
     */
    @Test
    @DisplayName("Сохраняет партию, если источник вернул null вместо следующей фигуры")
    void preservesStateWhenSourceReturnsNull() {
        // Arrange
        AtomicInteger calls = new AtomicInteger();
        TetrisGame game = new TetrisGame(4, 4,
                () -> calls.incrementAndGet() == 1 ? new O() : null);
        moveDown(game, 2);
        GameSnapshot before = game.snapshot();

        // Act
        assertThrows(NullPointerException.class, () -> game.execute(Command.DOWN));

        // Assert
        assertEquals(before, game.snapshot());
    }

    /**
     * Создаёт источник квадратов с однократным отказом на заданном обращении.
     *
     * @param failingCall номер обращения, на котором источник выбрасывает исключение
     * @return независимый источник с восстанавливающейся ошибкой
     */
    private static PieceSource failingOnceAt(int failingCall) {
        AtomicInteger calls = new AtomicInteger();
        return () -> {
            if (calls.incrementAndGet() == failingCall) {
                throw new IllegalStateException("Источник временно недоступен");
            }
            return new O();
        };
    }
}
