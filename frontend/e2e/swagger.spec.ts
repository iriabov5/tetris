import { test, expect } from '@playwright/test';

test('Swagger — создаёт партию и выполняет команду через Try it out', async ({ page }) => {
  // Arrange
  const origin = 'http://127.0.0.1:8081';
  const externalRequests: string[] = [];
  page.on('request', (request) => {
    if (new URL(request.url()).origin !== origin) externalRequests.push(request.url());
  });
  await page.goto(`${origin}/swagger-ui.html`);
  await expect(page.getByRole('heading', { name: 'Tetris API', exact: false })).toBeVisible();
  const creation = page.locator('[id$="-createGame"]');
  const command = page.locator('[id$="-executeCommand"]');

  // Act
  await creation.locator('.opblock-summary').click();
  await creation.getByRole('button', { name: 'Try it out' }).click();
  const createdResponse = page.waitForResponse(
    (response) =>
      response.url() === `${origin}/api/games` && response.request().method() === 'POST',
  );
  await creation.getByRole('button', { name: 'Execute', exact: true }).click();
  const created = await createdResponse;
  const state = (await created.json()) as { gameId: string };
  await command.locator('.opblock-summary').click();
  await command.getByRole('button', { name: 'Try it out' }).click();
  await command.getByPlaceholder('id', { exact: true }).fill(state.gameId);
  await command.locator('textarea.body-param__text').fill('{"command":"LEFT"}');
  const executedResponse = page.waitForResponse(
    (response) => response.url() === `${origin}/api/games/${state.gameId}/commands`,
  );
  await command.getByRole('button', { name: 'Execute', exact: true }).click();
  const executed = await executedResponse;
  const outcome = (await executed.json()) as { result: string; state: { gameId: string } };

  // Assert
  expect(created.status()).toBe(201);
  expect(created.headers()['location']).toBe(`/api/games/${state.gameId}`);
  expect(executed.status()).toBe(200);
  expect(outcome.result).toBe('APPLIED');
  expect(outcome.state.gameId).toBe(state.gameId);
  await expect(command.locator('.responses-wrapper')).toContainText(state.gameId);
  expect(externalRequests).toEqual([]);
});
