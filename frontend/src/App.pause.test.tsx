import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import {
  advanceClock,
  clickButton,
  fetchMock,
  initialGame,
  installFetchMock,
  renderFinishedGame,
  renderRunningGame,
  resetFetchMock,
  respondWith,
} from './test/game-fixtures';

beforeEach(() => {
  installFetchMock();
  vi.useFakeTimers();
});
afterEach(() => {
  vi.restoreAllMocks();
  resetFetchMock();
});

describe('Экран — локальная пауза', () => {
  it('пауза недоступна до начала партии', () => {
    // Arrange
    render(<App />);

    // Act
    fireEvent.click(screen.getByRole('button', { name: 'Пауза' }));

    // Assert
    expect(screen.getByRole('button', { name: 'Пауза' })).toBeDisabled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('останавливает падение и игровые команды, сохраняя поле без HTTP-запросов', async () => {
    // Arrange
    await renderRunningGame();
    const before = screen.getByTestId('board').innerHTML;
    await advanceClock(699);

    // Act
    fireEvent.click(screen.getByRole('button', { name: 'Пауза' }));
    fireEvent.click(screen.getByRole('button', { name: 'Влево' }));
    fireEvent.click(screen.getByRole('button', { name: 'Повернуть' }));
    await advanceClock(3000);

    // Assert
    expect(screen.getByRole('status')).toHaveTextContent('Пауза');
    expect(screen.getByRole('button', { name: 'Продолжить' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.getByRole('button', { name: 'Влево' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Вправо' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Повернуть' })).toBeDisabled();
    expect(screen.getByTestId('board').innerHTML).toBe(before);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('уже запланированный тик после нажатия паузы не отправляет DOWN', async () => {
    // Arrange
    const interval = vi.spyOn(window, 'setInterval');
    await renderRunningGame();
    const queuedTick = interval.mock.calls.at(-1)![0] as () => void;
    interval.mockRestore();
    fireEvent.click(screen.getByRole('button', { name: 'Пауза' }));

    // Act
    await act(async () => queuedTick());

    // Assert
    expect(screen.getByRole('status')).toHaveTextContent('Пауза');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('после продолжения ждёт полный интервал до следующего DOWN', async () => {
    // Arrange
    await renderRunningGame();
    await advanceClock(500);
    fireEvent.click(screen.getByRole('button', { name: 'Пауза' }));
    await advanceClock(3000);
    respondWith({ result: 'APPLIED', state: initialGame });

    // Act
    fireEvent.click(screen.getByRole('button', { name: 'Продолжить' }));
    await advanceClock(699);
    const callsBeforeInterval = fetchMock.mock.calls.length;
    await advanceClock(1);

    // Assert
    expect(callsBeforeInterval).toBe(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(JSON.parse(fetchMock.mock.calls.at(-1)![1]!.body as string)).toEqual({
      command: 'DOWN',
    });
    expect(screen.getByRole('button', { name: 'Пауза' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('button', { name: 'Влево' })).toBeEnabled();
  });

  it('смена скорости на паузе сохраняет остановку и применяется после продолжения', async () => {
    // Arrange
    await renderRunningGame();
    fireEvent.click(screen.getByRole('button', { name: 'Пауза' }));

    // Act
    fireEvent.change(screen.getByLabelText('Скорость'), { target: { value: '350' } });
    await advanceClock(3000);
    const callsOnPause = fetchMock.mock.calls.length;
    respondWith({ result: 'APPLIED', state: initialGame });
    fireEvent.click(screen.getByRole('button', { name: 'Продолжить' }));
    await advanceClock(349);
    const callsBeforeInterval = fetchMock.mock.calls.length;
    await advanceClock(1);

    // Assert
    expect(callsOnPause).toBe(1);
    expect(callsBeforeInterval).toBe(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('успешная новая игра снимает паузу и запускает падение', async () => {
    // Arrange
    await renderRunningGame();
    fireEvent.click(screen.getByRole('button', { name: 'Пауза' }));
    respondWith({ ...initialGame, gameId: 'new-game' }, 201);

    // Act
    await clickButton('Новая игра');
    respondWith({ result: 'APPLIED', state: { ...initialGame, gameId: 'new-game' } });
    await advanceClock(700);

    // Assert
    expect(screen.getByRole('status')).toHaveTextContent('Партия идёт');
    expect(screen.getByRole('button', { name: 'Пауза' })).toBeEnabled();
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls.at(-1)![0]).toBe('/api/games/new-game/commands');
  });

  it('ошибка создания новой игры сохраняет прежнюю партию на паузе', async () => {
    // Arrange
    await renderRunningGame();
    fireEvent.click(screen.getByRole('button', { name: 'Пауза' }));
    fetchMock.mockRejectedValueOnce(new TypeError('Network'));

    // Act
    await clickButton('Новая игра');
    await advanceClock(3000);

    // Assert
    expect(screen.getByRole('alert')).toBeVisible();
    expect(screen.getByRole('status')).toHaveTextContent('Пауза');
    expect(screen.getByRole('button', { name: 'Продолжить' })).toBeEnabled();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('не разрешает паузу во время запроса и при необходимости восстановления', async () => {
    // Arrange
    await renderRunningGame();
    let reject!: (reason: Error) => void;
    fetchMock.mockImplementationOnce(
      () =>
        new Promise<Response>((_resolve, fail) => {
          reject = fail;
        }),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Повернуть' }));
    const disabledDuringRequest = screen
      .getByRole('button', { name: 'Пауза' })
      .hasAttribute('disabled');

    // Act
    await act(async () => reject(new TypeError('Network')));
    fireEvent.click(screen.getByRole('button', { name: 'Пауза' }));
    await advanceClock(3000);

    // Assert
    expect(disabledDuringRequest).toBe(true);
    expect(screen.getByRole('button', { name: 'Пауза' })).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent('Нужно обновить состояние');
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('завершённая партия не может быть поставлена на паузу', async () => {
    // Arrange
    await renderFinishedGame();

    // Act
    fireEvent.click(screen.getByRole('button', { name: 'Пауза' }));
    await advanceClock(3000);

    // Assert
    expect(screen.getByRole('button', { name: 'Пауза' })).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent('Партия окончена');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
