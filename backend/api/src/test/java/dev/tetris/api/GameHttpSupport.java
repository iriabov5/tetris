package dev.tetris.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Поднимает HTTP-сервер на свободном порту для независимых групп API-проверок.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class GameHttpSupport {
    /**
     * Создаёт независимый экземпляр группы проверок.
     */
    GameHttpSupport() {
    }

    /**
     * Порт сервера, созданного Spring для текущего тестового контекста.
     */
    @LocalServerPort
    private int port;

    /**
     * Создаёт HTTP-клиент текущего сервера без зависимости от других тестов.
     *
     * @return клиент для запросов к текущему контексту
     */
    GameApiClient client() {
        return new GameApiClient(port);
    }
}
