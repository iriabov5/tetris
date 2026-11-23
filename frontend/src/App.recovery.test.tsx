import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import App from './App';
import {
  clickButton,
  initialGame,
  fetchMock,
  installFetchMock,
  resetFetchMock,
  respondWith,
  renderRunningGame,
} from './test/game-fixtures';

beforeEach(installFetchMock);
afterEach(resetFetchMock);

describe('Экран — ошибки и восстановление', () => {
  it('показывает ошибку создания партии', async () => {
    // Arrange
    respondWith({}, 500);
    render(<App />);

    // Act
    await clickButton('Новая игра');

    // Assert
    expect(screen.getByRole('alert')).toHaveTextContent('Не удалось связаться с сервером.');
    expect(screen.getByRole('button', { name: 'Новая игра' })).toBeEnabled();
  });

  it('сохраняет последний снимок и блокирует команды после сетевого сбоя', async () => {
    // Arrange
    await renderRunningGame();
    fetchMock.mockRejectedValueOnce(new TypeError('Network'));

    // Act
    await clickButton('Повернуть');

    // Assert
    expect(screen.getByRole('alert')).toBeVisible();
    expect(screen.getByTestId('board').querySelectorAll('[data-cell="active"]')).toHaveLength(4);
    expect(screen.getByRole('button', { name: 'Повернуть' })).toBeDisabled();
  });

  it('восстанавливает состояние чтением GET без повторной отправки команды', async () => {
    // Arrange
    await renderRunningGame();
    fetchMock.mockRejectedValueOnce(new TypeError('Network'));
    await clickButton('Повернуть');
    respondWith({ ...initialGame, clearedLines: 1 });

    // Act
    await clickButton('Обновить состояние');

    // Assert
    expect(fetchMock.mock.calls.at(-1)![1]?.method).toBe('GET');
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(screen.getByTestId('cleared-lines')).toHaveTextContent('1');
    expect(screen.getByRole('button', { name: 'Повернуть' })).toBeEnabled();
  });
});
