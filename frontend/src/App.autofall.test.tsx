import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import {
  clickButton,
  initialGame,
  fetchMock,
  installFetchMock,
  resetFetchMock,
  respondWith,
  renderRunningGame,
  advanceClock,
} from './test/game-fixtures';

beforeEach(installFetchMock);
afterEach(resetFetchMock);

describe('Экран — таймер автоматического падения', () => {
  beforeEach(() => vi.useFakeTimers());

  it.each([
    ['Спокойно', 1100],
    ['Обычно', 700],
    ['Быстро', 350],
  ])('скорость «%s» отправляет DOWN через %i мс', async (_label, interval) => {
    // Arrange
    await renderRunningGame();
    fireEvent.change(screen.getByLabelText('Скорость'), { target: { value: String(interval) } });
    respondWith({ result: 'APPLIED', state: initialGame });
    await advanceClock(interval - 1);
    const callsBeforeInterval = fetchMock.mock.calls.length;

    // Act
    await advanceClock(1);

    // Assert
    expect(callsBeforeInterval).toBe(1);
    expect(JSON.parse(fetchMock.mock.calls.at(-1)![1]!.body as string)).toEqual({
      command: 'DOWN',
    });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole('button', { name: 'Шаг вниз' })).not.toBeInTheDocument();
  });

  it('смена скорости перезапускает интервал без преждевременного шага', async () => {
    // Arrange
    await renderRunningGame();
    await advanceClock(500);
    respondWith({ result: 'APPLIED', state: initialGame });

    // Act
    fireEvent.change(screen.getByLabelText('Скорость'), { target: { value: '350' } });
    await advanceClock(349);
    const beforeNewInterval = fetchMock.mock.calls.length;
    await advanceClock(1);

    // Assert
    expect(beforeNewInterval).toBe(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('пропускает тики во время запроса без накопления очереди', async () => {
    // Arrange
    await renderRunningGame();
    let resolve!: (response: Response) => void;
    fetchMock.mockImplementationOnce(
      () =>
        new Promise<Response>((done) => {
          resolve = done;
        }),
    );

    // Act
    await advanceClock(2800);
    const callsDuringRequest = fetchMock.mock.calls.length;
    await act(async () =>
      resolve(new Response(JSON.stringify({ result: 'APPLIED', state: initialGame }))),
    );

    // Assert
    expect(callsDuringRequest).toBe(2);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('останавливает падение после ошибки команды', async () => {
    // Arrange
    await renderRunningGame();
    fetchMock.mockRejectedValueOnce(new TypeError('Network'));
    await advanceClock(700);
    const callsAfterError = fetchMock.mock.calls.length;

    // Act
    await advanceClock(3000);

    // Assert
    expect(screen.getByRole('alert')).toBeVisible();
    expect(fetchMock).toHaveBeenCalledTimes(callsAfterError);
  });

  it('возобновляет падение после успешного чтения состояния', async () => {
    // Arrange
    await renderRunningGame();
    fetchMock.mockRejectedValueOnce(new TypeError('Network'));
    await advanceClock(700);
    respondWith(initialGame);
    await clickButton('Обновить состояние');
    respondWith({ result: 'APPLIED', state: initialGame });

    // Act
    await advanceClock(700);

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(4);
    expect(JSON.parse(fetchMock.mock.calls.at(-1)![1]!.body as string)).toEqual({
      command: 'DOWN',
    });
  });

  it('останавливает падение после окончания партии', async () => {
    // Arrange
    await renderRunningGame();
    respondWith({
      result: 'APPLIED',
      state: { ...initialGame, status: 'GAME_OVER', active: null },
    });
    await advanceClock(700);

    // Act
    await advanceClock(3000);

    // Assert
    expect(screen.getByText('Партия окончена')).toBeVisible();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('удаляет таймер при закрытии экрана', async () => {
    // Arrange
    respondWith(initialGame, 201);
    const view = render(<App />);
    await clickButton('Новая игра');

    // Act
    view.unmount();
    await advanceClock(3000);

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
