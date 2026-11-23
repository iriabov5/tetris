package dev.tetris.core;

import dev.tetris.core.piece.O;
import dev.tetris.core.piece.T;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static dev.tetris.core.CoreTestFixtures.moveDown;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Мгновенный сброс — посадка, очистка и атомарность при ошибках.
 */
@DisplayName("Мгновенный сброс — посадка и атомарность")
class TetrisGameHardDropTest {
    /**
     * Создаёт независимую группу сценариев сброса.
     */
    TetrisGameHardDropTest() {
    }

    /**
     * Одна команда опускает к дну, закрепляет и выпускает следующую фигуру.
     */
    @Test
    @DisplayName("Одна команда опускает к дну, закрепляет и выпускает следующую фигуру")
    void dropsToFloor() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 6, O::new);
        var initial = game.snapshot().active();

        // Act
        CommandOutcome outcome = game.execute(Command.HARD_DROP);

        // Assert
        assertEquals(CommandResult.APPLIED, outcome.result());
        assertEquals(Set.of(new Cell(1, 4), new Cell(2, 4), new Cell(1, 5), new Cell(2, 5)), Set.copyOf(outcome.state().settled()));
        assertEquals(initial, outcome.state().active());
    }

    /**
     * Сброс останавливается на ближайшем препятствии.
     */
    @Test
    @DisplayName("Сброс останавливается на ближайшем препятствии")
    void stopsAboveSettledCells() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 8, O::new);
        game.execute(Command.HARD_DROP);

        // Act
        GameSnapshot state = game.execute(Command.HARD_DROP).state();

        // Assert
        assertEquals(Set.of(new Cell(1, 4), new Cell(2, 4), new Cell(1, 5), new Cell(2, 5), new Cell(1, 6), new Cell(2, 6), new Cell(1, 7), new Cell(2, 7)), Set.copyOf(state.settled()));
        assertEquals(GameStatus.RUNNING, state.status());
    }

    /**
     * Нулевая дистанция сброса сразу закрепляет фигуру.
     */
    @Test
    @DisplayName("Нулевая дистанция сброса сразу закрепляет фигуру")
    void locksAtZeroDistance() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        moveDown(game, 2);

        // Act
        GameSnapshot state = game.execute(Command.HARD_DROP).state();

        // Assert
        assertEquals(Set.of(new Cell(1, 2), new Cell(2, 2), new Cell(1, 3), new Cell(2, 3)), Set.copyOf(state.settled()));
        assertEquals(0, state.active().cells().stream().mapToInt(Cell::y).min().orElseThrow());
    }

    /**
     * Сброс сохраняет повёрнутую геометрию T.
     */
    @Test
    @DisplayName("Сброс сохраняет повёрнутую геометрию T")
    void dropsRotatedPiece() {
        // Arrange
        TetrisGame game = new TetrisGame(6, 6, T::new);
        game.execute(Command.ROTATE);

        // Act
        GameSnapshot state = game.execute(Command.HARD_DROP).state();

        // Assert
        assertEquals(Set.of(new Cell(3, 4), new Cell(2, 3), new Cell(2, 4), new Cell(2, 5)), Set.copyOf(state.settled()));
    }

    /**
     * Сброс удаляет две строки до выпуска следующей фигуры.
     */
    @Test
    @DisplayName("Сброс удаляет две строки до выпуска следующей фигуры")
    void clearsRowsBeforeSpawn() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        game.execute(Command.LEFT);
        game.execute(Command.HARD_DROP);
        game.execute(Command.RIGHT);

        // Act
        GameSnapshot state = game.execute(Command.HARD_DROP).state();

        // Assert
        assertEquals(2, state.clearedLines());
        assertTrue(state.settled().isEmpty());
        assertEquals(GameStatus.RUNNING, state.status());
        assertEquals(4, state.active().cells().size());
    }

    /**
     * Переполнение после сброса завершает игру, повтор возвращает FINISHED.
     */
    @Test
    @DisplayName("Переполнение после сброса завершает игру, повтор возвращает FINISHED")
    void finishesAndIgnoresFurtherDrops() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 4, O::new);
        game.execute(Command.HARD_DROP);

        // Act
        GameSnapshot finished = game.execute(Command.HARD_DROP).state();
        CommandOutcome repeated = game.execute(Command.HARD_DROP);

        // Assert
        assertEquals(GameStatus.GAME_OVER, finished.status());
        assertNull(finished.active());
        assertEquals(8, finished.settled().size());
        assertEquals(CommandResult.FINISHED, repeated.result());
        assertEquals(finished, repeated.state());
    }

    /**
     * Ошибка источника сохраняет исходную позицию, поле и счётчик.
     */
    @Test
    @DisplayName("Ошибка источника сохраняет исходную позицию, поле и счётчик")
    void preservesStateOnSourceFailure() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 6, failingOnce());
        GameSnapshot before = game.snapshot();

        // Act
        assertThrows(IllegalStateException.class, () -> game.execute(Command.HARD_DROP));

        // Assert
        assertEquals(before, game.snapshot());
    }

    /**
     * Повтор после восстановления закрепляет одну фигуру.
     */
    @Test
    @DisplayName("Повтор после восстановления закрепляет одну фигуру")
    void retriesAfterRecovery() {
        // Arrange
        TetrisGame game = new TetrisGame(4, 6, failingOnce());
        assertThrows(IllegalStateException.class, () -> game.execute(Command.HARD_DROP));

        // Act
        GameSnapshot state = game.execute(Command.HARD_DROP).state();

        // Assert
        assertEquals(4, state.settled().size());
        assertEquals(GameStatus.RUNNING, state.status());
        assertTrue(state.active().cells().stream().noneMatch(state.settled()::contains));
    }

    /**
     * Создаёт источник с однократным отказом при запросе второй фигуры.
     *
     * @return восстанавливающийся источник квадратов
     */
    private static PieceSource failingOnce() {
        AtomicInteger calls = new AtomicInteger();
        return () -> {
            if (calls.incrementAndGet() == 2) throw new IllegalStateException("Источник недоступен");
            return new O();
        };
    }
}
