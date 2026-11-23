package dev.tetris.core;

import dev.tetris.core.piece.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Общие исходные данные и последовательность шагов для проверок правил игры.
 */
final class CoreTestFixtures {
    /**
     * Исключает создание экземпляров набора фикстур.
     */
    private CoreTestFixtures() {
    }

    /**
     * Выдаёт каждый стандартный тип с читаемым именем параметризованного сценария.
     *
     * @return семь пар из имени типа и его определения
     */
    static Stream<Arguments> standardTypes() {
        return List.of(new I(), new O(), new T(), new S(), new Z(), new J(), new L())
                .stream().map(type -> Arguments.of(type.kind(), type));
    }

    /**
     * Подготавливает позицию партии заданным числом обычных команд DOWN.
     *
     * @param game  подготавливаемая партия
     * @param steps число последовательных шагов
     */
    static void moveDown(TetrisGame game, int steps) {
        for (int step = 0; step < steps; step++) {
            game.execute(Command.DOWN);
        }
    }
}
