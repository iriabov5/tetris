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
  renderFinishedGame,
} from './test/game-fixtures';

beforeEach(installFetchMock);
afterEach(resetFetchMock);

describe('Экран — создание и отображение партии', () => {
  it('показывает пустое поле и недоступное управление до начала', () => {
    // Arrange
    render(<App />);

    // Act
    const board = screen.getByRole('grid');

    // Assert
    expect(board.querySelectorAll('[data-cell="active"]')).toHaveLength(0);
    expect(screen.getAllByRole('row')).toHaveLength(20);
    expect(screen.getAllByRole('gridcell')).toHaveLength(200);
    expect(screen.getByRole('button', { name: 'Влево' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Новая игра' })).toBeEnabled();
  });

  it('создаёт партию и отображает четыре активные клетки', async () => {
    // Arrange
    respondWith(initialGame, 201);
    render(<App />);

    // Act
    await clickButton('Новая игра');

    // Assert
    expect(fetchMock.mock.calls[0][0]).toBe('/api/games');
    expect(fetchMock.mock.calls[0][1]?.method).toBe('POST');
    expect(screen.getByText('Партия идёт')).toBeVisible();
    expect(screen.getByTestId('board').querySelectorAll('[data-cell="active"]')).toHaveLength(4);
  });

  it('показывает итоговое поле и отключает команды завершённой партии', async () => {
    // Arrange
    respondWith(
      { ...initialGame, status: 'GAME_OVER', active: null, settled: initialGame.active!.cells },
      201,
    );
    render(<App />);

    // Act
    await clickButton('Новая игра');

    // Assert
    expect(screen.getByText('Партия окончена')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Влево' })).toBeDisabled();
    expect(screen.getByTestId('board').querySelectorAll('[data-cell="settled"]')).toHaveLength(4);
  });

  it('создаёт новую партию с пустым полем после окончания предыдущей', async () => {
    // Arrange
    await renderFinishedGame();
    respondWith(initialGame, 201);

    // Act
    await clickButton('Новая игра');

    // Assert
    expect(screen.getByText('Партия идёт')).toBeVisible();
    expect(screen.getByTestId('board').querySelectorAll('[data-cell="settled"]')).toHaveLength(0);
    expect(screen.getByTestId('cleared-lines')).toHaveTextContent('0');
  });
});
