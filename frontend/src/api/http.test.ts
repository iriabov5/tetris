import { afterEach, describe, expect, it, vi } from 'vitest';
import { createGame } from './generated/api.gen';

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('SDK — ошибки общего транспорта', () => {
  it('отклоняет HTTP 500 без повторной отправки POST', async () => {
    // Arrange
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify({ status: 500, title: 'Internal Server Error' }), {
        status: 500,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    // Act
    const result = createGame();

    // Assert
    await expect(result).rejects.toThrow('Сервер вернул ошибку 500.');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('передаёт сетевую ошибку без повторной отправки', async () => {
    // Arrange
    const failure = new TypeError('Network error');
    const fetchMock = vi.fn<typeof fetch>().mockRejectedValue(failure);
    vi.stubGlobal('fetch', fetchMock);

    // Act
    const result = createGame();

    // Assert
    await expect(result).rejects.toBe(failure);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('подключает таймаут 10 секунд и передаёт прерывание вызывающему коду', async () => {
    // Arrange
    const failure = new DOMException('Время ожидания истекло', 'TimeoutError');
    const signal = AbortSignal.abort(failure);
    const timeout = vi.spyOn(AbortSignal, 'timeout').mockReturnValue(signal);
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (_url, options) => {
      options?.signal?.throwIfAborted();
      return new Response('{}');
    });
    vi.stubGlobal('fetch', fetchMock);

    // Act
    const result = createGame();

    // Assert
    await expect(result).rejects.toBe(failure);
    expect(timeout).toHaveBeenCalledWith(10000);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][1]?.signal).toBe(signal);
  });
});
