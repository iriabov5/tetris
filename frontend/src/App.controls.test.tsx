import { act, fireEvent, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
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

describe('Экран — команды управления', () => {
  it.each([
    ['Влево', 'LEFT'],
    ['Вправо', 'RIGHT'],
    ['Повернуть', 'ROTATE'],
    ['Опустить', 'HARD_DROP'],
  ])('кнопка «%s» отправляет команду %s', async (label, command) => {
    // Arrange
    await renderRunningGame();
    respondWith({ result: 'APPLIED', state: { ...initialGame, clearedLines: 2 } });

    // Act
    await clickButton(label);

    // Assert
    expect(JSON.parse(fetchMock.mock.calls.at(-1)![1]!.body as string)).toEqual({ command });
    expect(screen.getByTestId('cleared-lines')).toHaveTextContent('2');
  });

  it('блокирует кнопки, пока команда ожидает ответа', async () => {
    // Arrange
    await renderRunningGame();
    fetchMock.mockImplementationOnce(() => new Promise<Response>(() => {}));

    // Act
    fireEvent.click(screen.getByRole('button', { name: 'Повернуть' }));

    // Assert
    expect(screen.getByRole('button', { name: 'Новая игра' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Влево' })).toBeDisabled();
  });

  it('повторное нажатие недоступной кнопки не отправляет вторую команду', async () => {
    // Arrange
    await renderRunningGame();
    fetchMock.mockImplementationOnce(() => new Promise<Response>(() => {}));
    fireEvent.click(screen.getByRole('button', { name: 'Повернуть' }));

    // Act
    fireEvent.click(screen.getByRole('button', { name: 'Повернуть' }));

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('разблокирует кнопки после получения ответа на команду', async () => {
    // Arrange
    await renderRunningGame();
    let resolve!: (response: Response) => void;
    fetchMock.mockImplementationOnce(
      () =>
        new Promise<Response>((done) => {
          resolve = done;
        }),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Повернуть' }));

    // Act
    await act(async () =>
      resolve(new Response(JSON.stringify({ result: 'APPLIED', state: initialGame }))),
    );

    // Assert
    expect(screen.getByRole('button', { name: 'Новая игра' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Влево' })).toBeEnabled();
  });

  it('объясняет заблокированный ход по ответу сервера', async () => {
    // Arrange
    await renderRunningGame();
    respondWith({ result: 'BLOCKED', state: initialGame });

    // Act
    await clickButton('Влево');

    // Assert
    expect(screen.getByText('Ход заблокирован. Попробуйте другое действие.')).toBeVisible();
  });
});
