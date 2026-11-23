import { useCallback, useEffect, useRef, useState } from 'react';
import { createGame, readGame, executeCommand } from './api/generated/api.gen';
import type { CommandBodyCommand as Command, GameSnapshot } from './api/generated/models';
import './styles.css';
import { useKeyboardControls } from './useKeyboardControls';

const commands: { command: Command; label: string; key: string; hint: string }[] = [
  { command: 'LEFT', label: 'Влево', key: 'ArrowLeft', hint: '←' },
  { command: 'RIGHT', label: 'Вправо', key: 'ArrowRight', hint: '→' },
  { command: 'ROTATE', label: 'Повернуть', key: 'ArrowUp', hint: '↑' },
  { command: 'HARD_DROP', label: 'Опустить', key: 'Space', hint: 'Пробел' },
];

const cellLabels: Record<string, string> = {
  empty: 'Пустая клетка',
  active: 'Активная фигура',
  settled: 'Закреплённая клетка',
};

export default function App() {
  const [game, setGame] = useState<GameSnapshot | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [needsRefresh, setNeedsRefresh] = useState(false);
  const [notice, setNotice] = useState('');
  const [paused, setPaused] = useState(false);
  const pausedRef = useRef(false);
  const requestInFlight = useRef(false);
  const [fallInterval, setFallInterval] = useState(700);
  const gameId = game?.gameId;
  const boardRef = useRef<HTMLDivElement>(null);

  const run = useCallback(
    async (action: 'new' | 'refresh' | Command) => {
      // Ref закрывает повторный вызов до того, как React обновит disabled у кнопок.
      if (requestInFlight.current) return;
      if (pausedRef.current && action !== 'new' && action !== 'refresh') return;
      requestInFlight.current = true;
      setBusy(true);
      setError(null);
      setNotice('');
      try {
        if (action === 'new') {
          setGame(await createGame());
          pausedRef.current = false;
          setPaused(false);
          setNeedsRefresh(false);
          boardRef.current?.focus({ preventScroll: true });
        } else if (gameId && action === 'refresh') {
          setGame(await readGame(gameId));
          setNeedsRefresh(false);
        } else if (gameId) {
          const outcome = await executeCommand(gameId, { command: action as Command });
          setGame(outcome.state);
          if (outcome.result === 'BLOCKED')
            setNotice('Ход заблокирован. Попробуйте другое действие.');
        }
      } catch {
        setError('Не удалось связаться с сервером.');
        if (gameId && action !== 'new') setNeedsRefresh(true);
      } finally {
        requestInFlight.current = false;
        setBusy(false);
      }
    },
    [gameId],
  );

  useEffect(() => {
    if (!gameId || game?.status !== 'RUNNING' || needsRefresh || paused) return;
    // Тики во время запроса пропускаются, очередь падений не накапливается.
    const timer = window.setInterval(() => void run('DOWN'), fallInterval);
    return () => window.clearInterval(timer);
  }, [gameId, game?.status, needsRefresh, paused, fallInterval, run]);

  const width = game?.width ?? 10;
  const height = game?.height ?? 20;
  const active = new Set(game?.active?.cells.map(({ x, y }) => `${x},${y}`));
  const settled = new Set(game?.settled.map(({ x, y }) => `${x},${y}`));
  const finished = game?.status === 'GAME_OVER';
  const unavailable = !game || busy || needsRefresh || finished;
  const disabled = unavailable || paused;

  const togglePause = useCallback(() => {
    if (unavailable || requestInFlight.current) return;
    // Ref блокирует уже запланированный тик до очистки интервала эффектом.
    pausedRef.current = !pausedRef.current;
    setPaused(pausedRef.current);
  }, [unavailable]);

  useKeyboardControls(Boolean(game) && !finished, !unavailable, run, togglePause);

  let status = 'Готовы начать?';
  if (busy) status = 'Обновляем поле…';
  else if (needsRefresh) status = 'Нужно обновить состояние';
  else if (finished) status = 'Партия окончена';
  else if (paused) status = 'Пауза';
  else if (game) status = 'Партия идёт';

  return (
    <>
      <header className="header">
        <span className="mark" aria-hidden="true" />
        <span className="brand">Тетрис</span>
      </header>
      <main className="game-layout">
        <section aria-label="Игровое поле">
          <div
            ref={boardRef}
            tabIndex={0}
            className="board"
            role="grid"
            aria-rowcount={height}
            aria-colcount={width}
            aria-label={`Поле ${width} на ${height}. Активных клеток ${active.size}, закреплённых ${settled.size}.`}
            style={{ gridTemplateColumns: `repeat(${width}, minmax(0, 1fr))` }}
            data-testid="board"
          >
            {Array.from({ length: height }, (_, y) => (
              <div key={y} className="board-row" role="row" aria-rowindex={y + 1}>
                {Array.from({ length: width }, (_, x) => {
                  const key = `${x},${y}`;
                  let type = 'empty';
                  if (active.has(key)) type = 'active';
                  else if (settled.has(key)) type = 'settled';
                  return (
                    <span
                      key={key}
                      className={`cell ${type}`}
                      data-cell={type}
                      role="gridcell"
                      aria-label={cellLabels[type]}
                      aria-colindex={x + 1}
                    />
                  );
                })}
              </div>
            ))}
          </div>
          <div className="legend" aria-hidden="true">
            <span>
              <i className="active" /> Фигура
            </span>
            <span>
              <i className="settled" /> На поле
            </span>
          </div>
        </section>
        <aside className="sidebar" aria-label="Управление партией">
          <h1>Собирайте строки</h1>

          <output className={`status ${finished ? 'finished' : ''}`}>{status}</output>
          <div className="stat">
            <span>Убрано строк</span>
            <strong data-testid="cleared-lines">{game?.clearedLines ?? 0}</strong>
          </div>
          <label className="speed-control">
            Скорость{' '}
            <select
              value={fallInterval}
              onChange={(event) => setFallInterval(Number(event.target.value))}
            >
              <option value={1100}>Спокойно</option>
              <option value={700}>Обычно</option>
              <option value={350}>Быстро</option>
            </select>
          </label>
          <div className="controls" aria-busy={busy}>
            {commands.map(({ command, label, key, hint }) => (
              <button
                type="button"
                key={command}
                className="shortcut-button"
                aria-keyshortcuts={key}
                disabled={disabled}
                onClick={() => void run(command)}
              >
                <span>{label}</span>
                <kbd className="key-hint" aria-hidden="true">
                  {hint}
                </kbd>
              </button>
            ))}
          </div>
          <button
            type="button"
            className="pause-game shortcut-button"
            aria-keyshortcuts="P Escape"
            disabled={unavailable}
            aria-pressed={paused}
            onClick={togglePause}
          >
            <span>{paused ? 'Продолжить' : 'Пауза'}</span>
            <kbd className="key-hint" aria-hidden="true">
              P / Esc
            </kbd>
          </button>
          <button
            type="button"
            className="new-game"
            disabled={busy}
            onClick={() => void run('new')}
          >
            Новая игра
          </button>
          {error && (
            <div className="error" role="alert">
              <p>{error}</p>
              {needsRefresh && (
                <p>Команда могла выполниться. Обновите поле перед следующим ходом.</p>
              )}
              {needsRefresh && (
                <button type="button" disabled={busy} onClick={() => void run('refresh')}>
                  Обновить состояние
                </button>
              )}
            </div>
          )}
          <p className="notice" aria-live="polite">
            {notice}
          </p>
        </aside>
      </main>
    </>
  );
}
