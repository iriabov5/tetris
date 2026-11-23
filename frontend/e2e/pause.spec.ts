import { test, expect } from '@playwright/test';
import { advanceFall, clickButton, openGameWithControlledClock } from './game-actions';

for (const width of [360, 768, 1440]) {
  test(`пауза и продолжение на экране ${width} px`, async ({ page }) => {
    // Arrange
    await page.setViewportSize({ width, height: 1000 });
    await openGameWithControlledClock(page);
    const before = await page.getByTestId('board').innerHTML();
    const commands: string[] = [];
    page.on('request', (request) => {
      if (request.url().endsWith('/commands')) commands.push(request.postData() ?? '');
    });

    // Act
    await clickButton(page, 'Пауза');
    await page.clock.runFor(3500);

    // Assert
    await expect(page.getByRole('status')).toHaveText('Пауза');
    await expect(page.getByRole('button', { name: 'Влево', exact: true })).toBeDisabled();
    await expect(page.getByRole('button', { name: 'Продолжить', exact: true })).toBeEnabled();
    expect(commands).toHaveLength(0);
    expect(await page.getByTestId('board').innerHTML()).toBe(before);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
    await page.screenshot({ path: `../docs/verification/paused-${width}.png`, fullPage: true });
    await clickButton(page, 'Продолжить');
    await page.clock.runFor(349);
    expect(commands).toHaveLength(0);
    const response = page.waitForResponse((result) => result.url().endsWith('/commands'));
    await page.clock.runFor(1);
    await response;
    await expect(page.getByRole('status')).toHaveText('Партия идёт');
    expect(commands).toHaveLength(1);
    expect(JSON.parse(commands[0])).toEqual({ command: 'DOWN' });
    await expect(page.getByRole('button', { name: 'Влево', exact: true })).toBeEnabled();
  });
}

test('пауза одной вкладки не останавливает игру другой', async ({ page, context }) => {
  // Arrange
  await openGameWithControlledClock(page);
  const other = await context.newPage();
  await openGameWithControlledClock(other);
  const before = await page.getByTestId('board').innerHTML();

  // Act
  await clickButton(page, 'Пауза');
  await advanceFall(other, 1);
  await page.clock.runFor(3500);

  // Assert
  await expect(page.getByRole('status')).toHaveText('Пауза');
  expect(await page.getByTestId('board').innerHTML()).toBe(before);
  await expect(other.getByRole('status')).toHaveText('Партия идёт');
  expect(await other.getByTestId('board').innerHTML()).not.toBe(before);
});
