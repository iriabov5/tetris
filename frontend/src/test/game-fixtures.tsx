import { act, fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import App from '../App';
import type { GameSnapshot } from '../api/generated/models';

export const initialGame: GameSnapshot = {
  gameId: '41fd4e78-9492-440d-a093-d1ddaa9a6e7d',
  width: 10,
  height: 20,
  status: 'RUNNING',
  settled: [],
  clearedLines: 0,
  active: {
    kind: 'O',
    cells: [
      { x: 4, y: 0 },
      { x: 5, y: 0 },
      { x: 4, y: 1 },
      { x: 5, y: 1 },
    ],
  },
};

export const fetchMock = vi.fn<typeof fetch>();

export function installFetchMock() {
  vi.stubGlobal('fetch', fetchMock);
}

export function resetFetchMock() {
  vi.unstubAllGlobals();
  vi.useRealTimers();
  fetchMock.mockReset();
}

export function respondWith(body: unknown, status = 200) {
  fetchMock.mockResolvedValueOnce(new Response(JSON.stringify(body), { status }));
}

export async function renderRunningGame() {
  respondWith(initialGame, 201);
  render(<App />);
  await clickButton('Новая игра');
}

export async function renderFinishedGame() {
  respondWith(
    { ...initialGame, status: 'GAME_OVER', active: null, settled: initialGame.active!.cells },
    201,
  );
  render(<App />);
  await clickButton('Новая игра');
}

export async function advanceClock(milliseconds: number) {
  await act(() => vi.advanceTimersByTimeAsync(milliseconds));
}

export async function clickButton(name: string) {
  fireEvent.click(screen.getByRole('button', { name }));
  // fireEvent завершает синхронное событие, а HTTP-ответ обновляет React позже.
  const response = fetchMock.mock.results.at(-1)!.value;
  await act(async () => {
    await Promise.allSettled([response]);
  });
}

export async function pressKey(key: string, options: KeyboardEventInit = {}) {
  fireEvent.keyDown(document.activeElement ?? document.body, { key, ...options });
  const response = fetchMock.mock.results.at(-1)?.value;
  await act(async () => {
    await Promise.allSettled([response]);
  });
}
