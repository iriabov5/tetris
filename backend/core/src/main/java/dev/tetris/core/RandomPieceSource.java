package dev.tetris.core;

import dev.tetris.core.piece.*;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Источник, выбирающий тип из неизменяемого набора с допустимыми повторами.
 */
public final class RandomPieceSource implements PieceSource {
    /**
     * Копия набора типов для случайного выбора.
     */
    private final List<Tetromino> types;
    /**
     * Генератор индекса, подменяемый в детерминированных проверках.
     */
    private final RandomGenerator random;

    /**
     * Создаёт источник семи стандартных типов с генератором случайных чисел JDK.
     */
    public RandomPieceSource() {
        this(List.of(new I(), new O(), new T(), new S(), new Z(), new J(), new L()), RandomGenerator.getDefault());
    }

    /**
     * Позволяет подключить дополнительные типы и управляемый генератор.
     *
     * @param types  непустой набор определений фигур
     * @param random генератор индекса следующего типа
     * @throws IllegalArgumentException если набор типов пуст
     * @throws NullPointerException     если передан null вместо набора, его элемента или генератора
     */
    public RandomPieceSource(List<Tetromino> types, RandomGenerator random) {
        this.types = List.copyOf(types);
        this.random = Objects.requireNonNull(random);
        if (types.isEmpty()) {
            throw new IllegalArgumentException("Источник должен содержать фигуры");
        }
    }

    /**
     * Выбирает один тип с равной вероятностью для каждого элемента набора.
     *
     * @return определение фигуры, которое может совпасть с предыдущим
     */
    @Override
    public Tetromino next() {
        return types.get(random.nextInt(types.size()));
    }
}
