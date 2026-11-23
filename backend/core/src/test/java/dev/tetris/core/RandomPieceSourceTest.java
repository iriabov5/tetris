package dev.tetris.core;

import dev.tetris.core.piece.*;

import java.util.List;
import java.util.Set;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Источник — выбор следующего типа фигуры.
 */
@DisplayName("Источник — выбор следующего типа фигуры")
class RandomPieceSourceTest {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    RandomPieceSourceTest() {
    }

    /**
     * Допускает повторную выдачу единственного типа.
     */
    @Test
    @DisplayName("Допускает повторную выдачу единственного типа")
    void allowsRepeatedTypes() {
        // Arrange
        RandomPieceSource source = new RandomPieceSource(List.of(new O()), new Random(1));

        // Act
        List<String> kinds = List.of(source.next().kind(), source.next().kind());

        // Assert
        assertEquals(List.of("O", "O"), kinds);
    }

    /**
     * Рабочий источник выдаёт один из семи стандартных типов.
     */
    @Test
    @DisplayName("Рабочий источник выдаёт один из семи стандартных типов")
    void providesStandardType() {
        // Arrange
        RandomPieceSource source = new RandomPieceSource();

        // Act
        String kind = source.next().kind();

        // Assert
        assertTrue(Set.of("I", "O", "T", "S", "Z", "J", "L").contains(kind));
    }

    /**
     * Выбирает тип по управляемому индексу из каталога с нестандартным порядком.
     *
     * @param index индекс, выбранный генератором
     * @param expectedKind ожидаемое имя фигуры из заданного каталога
     */
    @ParameterizedTest(name = "индекс {0} — фигура {1}")
    @CsvSource({"0, Z", "1, L", "2, I", "3, O", "4, J", "5, T", "6, S"})
    @DisplayName("Каждый индекс генератора выбирает соответствующий тип каталога")
    void selectsTypeAtGeneratedIndex(int index, String expectedKind) {
        // Arrange
        List<Tetromino> types = List.of(new Z(), new L(), new I(), new O(), new J(), new T(), new S());
        Random random = new Random(1) {
            /**
             * Возвращает заданный индекс для независимости проверки от случайной последовательности.
             *
             * @param bound верхняя исключающая граница индекса
             * @return заданный индекс в допустимом диапазоне
             */
            @Override
            public int nextInt(int bound) {
                return Math.floorMod(index, bound);
            }
        };
        RandomPieceSource source = new RandomPieceSource(types, random);

        // Act
        String kind = source.next().kind();

        // Assert
        assertEquals(expectedKind, kind);
    }

    /**
     * Отклоняет пустой набор типов.
     */
    @Test
    @DisplayName("Отклоняет пустой набор типов")
    void rejectsEmptyCatalog() {
        // Arrange
        List<Tetromino> types = List.of();
        Random random = new Random(1);

        // Act
        var failure = assertThrows(IllegalArgumentException.class,
                () -> new RandomPieceSource(types, random));

        // Assert
        assertEquals("Источник должен содержать фигуры", failure.getMessage());
    }

}
