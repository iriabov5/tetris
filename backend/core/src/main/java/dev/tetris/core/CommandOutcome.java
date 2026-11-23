package dev.tetris.core;

/**
 * Результат одной команды вместе со снимком после её обработки.
 *
 * @param result признак применённой, заблокированной или завершённой команды
 * @param state  снимок, сформированный внутри операции партии
 */
public record CommandOutcome(CommandResult result, GameSnapshot state) {
}
