package dev.tetris.api;

import dev.tetris.core.TetrisGame;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Реестр независимых партий в памяти текущего серверного процесса.
 */
@Service
public class GameRegistry {
    /**
     * Журнал жизненного цикла партий и ошибок поиска.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GameRegistry.class);

    /**
     * Потокобезопасное сопоставление UUID и экземпляров партий.
     */
    private final ConcurrentHashMap<UUID, TetrisGame> games = new ConcurrentHashMap<>();
    /**
     * Фабрика отдельного состояния для каждой создаваемой партии.
     */
    private final Supplier<TetrisGame> factory;

    /**
     * Принимает фабрику, создающую отдельный экземпляр партии на каждый вызов.
     *
     * @param factory фабрика для рабочего или тестового профиля
     */
    public GameRegistry(Supplier<TetrisGame> factory) {
        this.factory = factory;
    }

    /**
     * Создаёт и регистрирует партию под её идентификатором.
     *
     * @return новая партия, доступная для последующих команд
     * @throws GameOperationException если фабрика не смогла создать партию; причина сохраняется
     */
    public TetrisGame create() {
        TetrisGame game;
        try {
            game = factory.get();
        } catch (RuntimeException exception) {
            throw new GameOperationException("game_creation_failed", null, null, exception);
        }
        var state = game.snapshot();
        games.put(state.gameId(), game);
        LOGGER.info("event=game_created gameId={} width={} height={}",
                state.gameId(), state.width(), state.height());
        return game;
    }

    /**
     * Находит партию без изменения её состояния.
     *
     * @param id идентификатор из снимка партии
     * @return ранее созданная партия
     * @throws GameNotFoundException если реестр не содержит такого идентификатора
     */
    public TetrisGame get(UUID id) {
        TetrisGame game = games.get(id);
        if (game == null) {
            LOGGER.warn("event=game_missing gameId={}", id);
            throw new GameNotFoundException();
        }
        return game;
    }
}
