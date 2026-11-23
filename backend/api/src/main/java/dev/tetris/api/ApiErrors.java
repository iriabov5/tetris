package dev.tetris.api;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Единое преобразование ошибок HTTP и отсутствующей партии в problem+json.
 */
@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {
    /**
     * Единое место записи ERROR для необработанных сбоев операций партии.
     */
    private static final Logger OPERATION_LOG = LoggerFactory.getLogger(ApiErrors.class);

    /**
     * Создаёт общий обработчик ошибок HTTP-контракта.
     */
    public ApiErrors() {
        // Spring создаёт обработчик без зависимостей; настройка выполняется унаследованным классом.
    }

    /**
     * Преобразует отсутствие партии в ответ со статусом 404.
     *
     * @return стандартное описание проблемы с пояснением на русском
     */
    @ExceptionHandler(GameNotFoundException.class)
    public ProblemDetail missingGame() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Партия не найдена");
    }

    /**
     * Записывает сбой один раз с контекстом и возвращает безопасное описание HTTP 500.
     *
     * @param failure ошибка с контекстом и исходной причиной
     * @return problem+json со статусом 500 без внутренних деталей исключения
     */
    @ExceptionHandler(GameOperationException.class)
    public ProblemDetail failedOperation(GameOperationException failure) {
        OPERATION_LOG.error("event={} gameId={} command={}",
                failure.event(), failure.gameId(), failure.command(), failure);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Не удалось выполнить операцию партии");
    }

}
