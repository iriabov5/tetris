package dev.tetris.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка запуска HTTP-сервера Spring Boot с реестром партий и ядром игры.
 */
@SpringBootApplication
public class TetrisApplication {
    /**
     * Создаёт конфигурационный класс запуска Spring Boot.
     */
    public TetrisApplication() {
        // Spring создаёт конфигурацию без состояния; запуск выполняется методом main.
    }

    /**
     * Запускает Spring-контекст и встроенный HTTP-сервер.
     *
     * @param args параметры запуска, включая профиль и порт сервера
     */
    public static void main(String[] args) {
        SpringApplication.run(TetrisApplication.class, args);
    }
}
