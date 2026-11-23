/** Общий транспорт: маршруты, методы и JSON-тела задаёт сгенерированный SDK. */
export async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, { ...options, signal: AbortSignal.timeout(10000) });
  if (!response.ok) {
    throw new Error(`Сервер вернул ошибку ${response.status}.`);
  }
  return (await response.json()) as T;
}
