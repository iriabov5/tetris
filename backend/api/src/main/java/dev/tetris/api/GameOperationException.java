package dev.tetris.api;

import dev.tetris.core.Command;
import java.util.UUID;

/**
 * Передаёт контекст сбоя операции единому HTTP-обработчику, сохраняя исходную причину.
 */
final class GameOperationException extends RuntimeException {
    /**
     * Имя технического события создания или выполнения команды.
     */
    private final String event;
    /**
     * UUID партии либо null, если создать партию не удалось.
     */
    private final UUID gameId;
    /**
     * Команда либо null для операции создания.
     */
    private final Command command;

    /**
     * Добавляет контекст операции к исключению без записи в журнал на месте вызова.
     *
     * @param event имя события журнала
     * @param gameId UUID партии либо null до её создания
     * @param command выполнявшаяся команда либо null для создания
     * @param cause исходная ошибка операции
     */
    GameOperationException(String event, UUID gameId, Command command, RuntimeException cause) {
        super(event + " gameId=" + gameId + " command=" + command, cause);
        this.event = event;
        this.gameId = gameId;
        this.command = command;
    }

    /**
     * Возвращает имя события для единственной записи ERROR.
     *
     * @return имя события
     */
    String event() {
        return event;
    }

    /**
     * Возвращает идентификатор, доступный в момент сбоя.
     *
     * @return UUID партии либо null при сбое создания
     */
    UUID gameId() {
        return gameId;
    }

    /**
     * Возвращает выполнявшуюся команду.
     *
     * @return команда либо null для создания партии
     */
    Command command() {
        return command;
    }
}
