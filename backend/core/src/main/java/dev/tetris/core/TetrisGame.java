package dev.tetris.core;

import java.util.Objects;
import java.util.UUID;

/**
 * Управляет одной партией и согласует изменения поля, активной фигуры и статуса.
 * Команды и чтение снимка синхронизированы на экземпляре партии.
 * Внешние потребители получают неизменяемые снимки вместо доступа к полю.
 */
public final class TetrisGame {
    /**
     * Идентификатор, связывающий снимки одной партии.
     */
    private final UUID gameId = UUID.randomUUID();
    /**
     * Число столбцов поля.
     */
    private final int width;
    /**
     * Число строк поля.
     */
    private final int height;
    /**
     * Поле с закреплёнными клетками, принадлежащее этой партии.
     */
    private final Board board;
    /**
     * Источник определения очередной фигуры.
     */
    private final PieceSource source;
    /**
     * Текущее размещение или null после невозможного выпуска.
     */
    private ActivePiece active;
    /**
     * Текущий жизненный цикл партии.
     */
    private GameStatus status = GameStatus.RUNNING;
    /**
     * Суммарное число удалённых строк с начала партии.
     */
    private int clearedLines;

    /**
     * Создаёт партию 10 × 20 со случайным источником семи стандартных типов.
     */
    public TetrisGame() {
        this(10, 20, new RandomPieceSource());
    }

    /**
     * Создаёт партию с собственным источником и сразу выпускает первую фигуру.
     * Если стартовые клетки не помещаются, партия создаётся завершённой.
     *
     * @param width  положительная ширина поля
     * @param height положительная высота поля
     * @param source источник последующих определений фигур
     * @throws IllegalArgumentException если размеры поля неположительные
     * @throws NullPointerException     если источник равен null
     */
    public TetrisGame(int width, int height, PieceSource source) {
        this.width = width;
        this.height = height;
        this.board = new Board(width, height);
        this.source = Objects.requireNonNull(source);
        spawn(ActivePiece.spawn(source.next(), width));
    }

    /**
     * Фиксирует согласованное состояние партии под той же блокировкой, что и команды.
     *
     * @return независимый неизменяемый снимок текущего состояния
     */
    public synchronized GameSnapshot snapshot() {
        ActiveView view = active == null ? null : new ActiveView(active.type().kind(), active.cells());
        return new GameSnapshot(gameId, width, height, status, board.cells(), view, clearedLines);
    }

    /**
     * Выполняет сдвиг, поворот или мгновенный сброс и возвращает снимок результата.
     * Заблокированный DOWN и HARD_DROP закрепляют фигуру, очищают строки и выпускают следующую.
     * Боковой сдвиг и поворот при столкновении сохраняют прежнее размещение.
     * Ошибка источника следующей фигуры передаётся вызывающему коду без изменения партии.
     *
     * @param command действие для активной фигуры
     * @return результат APPLIED, BLOCKED или FINISHED вместе со снимком партии
     * @throws NullPointerException     если команда или очередное определение фигуры равны null
     * @throws IllegalArgumentException если подключённый тип вернул недопустимую геометрию поворота
     * @throws RuntimeException если источник следующей фигуры завершился ошибкой; состояние партии сохраняется
     */
    public synchronized CommandOutcome execute(Command command) {
        Objects.requireNonNull(command);
        if (status == GameStatus.GAME_OVER) {
            return new CommandOutcome(CommandResult.FINISHED, snapshot());
        }
        ActivePiece candidate = switch (command) {
            case LEFT -> active.translated(-1, 0);
            case RIGHT -> active.translated(1, 0);
            case DOWN -> active.translated(0, 1);
            case ROTATE -> active.rotated();
            case HARD_DROP -> landingPosition();
        };
        CommandResult result = CommandResult.APPLIED;
        if (command == Command.HARD_DROP) {
            lockAndSpawn(candidate);
        } else if (board.canPlace(candidate.cells())) {
            active = candidate;
        } else if (command == Command.DOWN) {
            lockAndSpawn(active);
        } else {
            result = CommandResult.BLOCKED;
        }
        return new CommandOutcome(result, snapshot());
    }

    /**
     * Находит посадку без изменения активного размещения и состояния поля.
     *
     * @return самое низкое допустимое размещение текущей фигуры
     */
    private ActivePiece landingPosition() {
        ActivePiece landing = active;
        ActivePiece lower = landing.translated(0, 1);
        while (board.canPlace(lower.cells())) {
            landing = lower;
            lower = landing.translated(0, 1);
        }
        return landing;
    }

    /**
     * Подготавливает следующий тип до изменения поля и выполняет общее закрепление.
     *
     * @param piece допустимое размещение, которое нужно закрепить
     * @throws RuntimeException если источник не смог подготовить следующий тип; партия сохраняется
     */
    private void lockAndSpawn(ActivePiece piece) {
        ActivePiece next = ActivePiece.spawn(source.next(), width);
        clearedLines += board.lock(piece.cells());
        spawn(next);
    }

    /**
     * Проверяет подготовленное размещение на текущем поле, включая результат очистки строк.
     *
     * @param next заранее подготовленная следующая фигура
     */
    private void spawn(ActivePiece next) {
        if (board.canPlace(next.cells())) {
            active = next;
        } else {
            active = null;
            status = GameStatus.GAME_OVER;
        }
    }
}
