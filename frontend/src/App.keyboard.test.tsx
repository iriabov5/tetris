import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import App from './App';
import {
  clickButton,
  fetchMock,
  initialGame,
  installFetchMock,
  pressKey,
  renderFinishedGame,
  renderRunningGame,
  resetFetchMock,
  respondWith,
} from './test/game-fixtures';

beforeEach(installFetchMock);
afterEach(resetFetchMock);

describe('Экран — клавиатура', () => {
  it.each([
    ['ArrowLeft', 'LEFT'],
    ['ArrowRight', 'RIGHT'],
    ['ArrowUp', 'ROTATE'],
    [' ', 'HARD_DROP'],
  ])('%s отправляет одну команду %s и отображает ответ', async (key, command) => {
    // Arrange
    await renderRunningGame();
    respondWith({ result: 'APPLIED', state: { ...initialGame, clearedLines: 2 } });

    // Act
    await pressKey(key);

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(JSON.parse(fetchMock.mock.calls.at(-1)![1]!.body as string)).toEqual({ command });
    expect(screen.getByTestId('cleared-lines')).toHaveTextContent('2');
    expect(screen.getByTestId('board')).toHaveFocus();
  });

  it.each([
    ['p', 'KeyP'],
    ['з', 'KeyP'],
    ['Escape', 'Escape'],
  ])('%s ставит на паузу и продолжает игру без запросов', async (key, code) => {
    // Arrange
    await renderRunningGame();

    // Act
    await pressKey(key, { code });
    const paused = screen.getByRole('status').textContent;
    await pressKey(' ');
    await pressKey('ArrowLeft');
    await pressKey(key, { code });

    // Assert
    expect(paused).toBe('Пауза');
    expect(screen.getByRole('status')).toHaveTextContent('Партия идёт');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it.each(['input', 'textarea', 'select', '[contenteditable]'])(
    'не перехватывает ввод в %s',
    async (selector) => {
      // Arrange
      await renderRunningGame();
      const fields = render(
        <div>
          <input />
          <textarea />
          <select>
            <option>Скорость</option>
          </select>
          <div contentEditable suppressContentEditableWarning>
            Текст
          </div>
        </div>,
      );
      const field = fields.container.querySelector(selector)!;
      const event = new KeyboardEvent('keydown', {
        key: 'ArrowLeft',
        bubbles: true,
        cancelable: true,
      });

      // Act
      field.dispatchEvent(event);

      // Assert
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(event.defaultPrevented).toBe(false);
    },
  );

  it.each(['ctrlKey', 'altKey', 'metaKey', 'shiftKey'])(
    'не перехватывает сочетание с %s',
    async (modifier) => {
      // Arrange
      await renderRunningGame();

      // Act
      await pressKey('ArrowLeft', { [modifier]: true });

      // Assert
      expect(fetchMock).toHaveBeenCalledTimes(1);
    },
  );

  it('не повторяет сброс и переключение паузы при удержании клавиш', async () => {
    // Arrange
    await renderRunningGame();

    // Act
    await pressKey(' ', { repeat: true });
    await pressKey('p', { code: 'KeyP', repeat: true });

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('status')).toHaveTextContent('Партия идёт');
  });

  it('игнорирует IME и уже обработанное событие', async () => {
    // Arrange
    await renderRunningGame();
    const event = new KeyboardEvent('keydown', { key: 'ArrowUp', bubbles: true, cancelable: true });
    event.preventDefault();

    // Act
    await pressKey('ArrowLeft', { isComposing: true });
    screen.getByTestId('board').dispatchEvent(event);

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('блокирует прокрутку поддерживаемой игровой клавиши', async () => {
    // Arrange
    await renderRunningGame();
    respondWith({ result: 'APPLIED', state: initialGame });
    const event = new KeyboardEvent('keydown', {
      key: 'ArrowLeft',
      bubbles: true,
      cancelable: true,
    });

    // Act
    screen.getByTestId('board').dispatchEvent(event);
    await pressKey('Unidentified');

    // Assert
    expect(event.defaultPrevented).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('не запускает команды и паузу до начала игры', async () => {
    // Arrange
    render(<App />);

    // Act
    await pressKey(' ');
    await pressKey('p', { code: 'KeyP' });

    // Assert
    expect(fetchMock).not.toHaveBeenCalled();
    expect(screen.getByRole('status')).toHaveTextContent('Готовы начать?');
  });

  it('не накапливает нажатия, пока выполняется запрос', async () => {
    // Arrange
    await renderRunningGame();
    fetchMock.mockImplementationOnce(() => new Promise<Response>(() => {}));

    // Act
    fireEvent.keyDown(screen.getByTestId('board'), { key: 'ArrowLeft' });
    const heldSpace = new KeyboardEvent('keydown', {
      key: ' ',
      repeat: true,
      bubbles: true,
      cancelable: true,
    });
    screen.getByTestId('board').dispatchEvent(heldSpace);
    fireEvent.keyDown(screen.getByTestId('board'), { key: 'ArrowRight' });

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(screen.getByRole('button', { name: 'Опустить' })).toBeDisabled();
    expect(heldSpace.defaultPrevented).toBe(true);
  });

  it('блокирует клавиатуру после ошибки до восстановления состояния', async () => {
    // Arrange
    await renderRunningGame();
    fetchMock.mockRejectedValueOnce(new TypeError('Network'));
    await pressKey('ArrowLeft');

    // Act
    await pressKey(' ');
    await pressKey('p', { code: 'KeyP' });

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(screen.getByRole('status')).toHaveTextContent('Нужно обновить состояние');
  });

  it('завершённая партия не принимает горячие клавиши', async () => {
    // Arrange
    await renderFinishedGame();

    // Act
    await pressKey(' ');
    await pressKey('ArrowUp');

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('status')).toHaveTextContent('Партия окончена');
  });

  it('после размонтирования обработчик клавиатуры удалён', async () => {
    // Arrange
    respondWith(initialGame, 201);
    const view = render(<App />);
    await clickButton('Новая игра');

    // Act
    view.unmount();
    await pressKey('ArrowLeft');

    // Assert
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
