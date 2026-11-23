import { test, expect } from '@playwright/test';
import { clickButton, openGameWithControlledClock } from './game-actions';

test('стрелки, Пробел, P и Esc управляют игрой через реальные запросы', async ({ page }) => {
  // Arrange
  await openGameWithControlledClock(page);
  const board = page.getByTestId('board');
  const commands: string[] = [];
  page.on('request', (request) => {
    if (request.url().endsWith('/commands')) commands.push(JSON.parse(request.postData()!).command);
  });

  // Act
  await page.keyboard.press('ArrowLeft');
  await expect(page.getByRole('button', { name: 'Опустить', exact: true })).toBeEnabled();
  await page.keyboard.press('ArrowUp');
  await expect(page.getByRole('button', { name: 'Опустить', exact: true })).toBeEnabled();
  await page.keyboard.press('Space');
  await expect(page.locator('[data-cell="settled"]')).toHaveCount(4);
  await page.keyboard.press('KeyP');
  await expect(page.getByRole('status')).toHaveText('Пауза');
  await page.keyboard.press('Space');
  await page.keyboard.press('Escape');

  // Assert
  await expect(page.getByRole('status')).toHaveText('Партия идёт');
  expect(commands).toEqual(['LEFT', 'ROTATE', 'HARD_DROP']);
  await expect(board).toBeFocused();
});

test('Пробел на кнопке сохраняет нативное нажатие, список скорости не перехватывается', async ({
  page,
}) => {
  // Arrange
  await openGameWithControlledClock(page);
  const requests: string[] = [];
  page.on('request', (request) => {
    if (request.url().endsWith('/commands')) requests.push(request.postData() ?? '');
  });

  // Act
  await page.getByLabel('Скорость').focus();
  await page.keyboard.press('ArrowUp');
  await page.getByRole('button', { name: 'Пауза', exact: true }).focus();
  await page.keyboard.press('Space');

  // Assert
  await expect(page.getByRole('status')).toHaveText('Пауза');
  expect(requests).toHaveLength(0);
});

for (const width of [360, 768, 1440]) {
  test(`кнопка опускает фигуру одним запросом на экране ${width} px`, async ({ page }) => {
    // Arrange
    await page.setViewportSize({ width, height: 1000 });
    await openGameWithControlledClock(page);
    const commands: string[] = [];
    page.on('request', (request) => {
      if (request.url().endsWith('/commands'))
        commands.push(JSON.parse(request.postData()!).command);
    });

    // Act
    await clickButton(page, 'Опустить');

    // Assert
    await expect(page.locator('[data-cell="settled"]')).toHaveCount(4);
    await expect(page.locator('[data-cell="active"]')).toHaveCount(4);
    expect(commands).toEqual(['HARD_DROP']);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
  });
}
