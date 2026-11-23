import { test, expect } from '@playwright/test';
import { openGameWithControlledClock } from './game-actions';

for (const width of [360, 768, 1440]) {
  test.describe(`Экран шириной ${width} px`, () => {
    test('помещается по ширине и сохраняет квадратные клетки', async ({ page }) => {
      // Arrange
      await page.setViewportSize({ width, height: 1000 });
      await openGameWithControlledClock(page);

      // Act
      const fitsWidth = await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      );
      const cell = await page.locator('[data-cell]').first().boundingBox();

      // Assert
      expect(fitsWidth).toBe(true);
      const hints = page.locator('button .key-hint');
      await expect(hints).toHaveCount(5);
      for (const hint of await hints.all()) {
        if (width <= 700) await expect(hint).toBeHidden();
        else await expect(hint).toBeVisible();
      }
      expect(Math.abs(cell!.width - cell!.height)).toBeLessThan(1);
      await page.screenshot({ path: `../docs/verification/running-${width}.png`, fullPage: true });
    });

    test('показывает ошибку и обновление состояния после обрыва команды', async ({ page }) => {
      // Arrange
      await page.setViewportSize({ width, height: 1000 });
      await openGameWithControlledClock(page);
      await page.route('**/commands', (route) => route.abort());

      // Act
      await page.clock.runFor(350);

      // Assert
      await expect(page.getByRole('alert')).toBeVisible();
      await expect(page.getByRole('button', { name: 'Обновить состояние' })).toBeEnabled();
      await page.screenshot({ path: `../docs/verification/error-${width}.png`, fullPage: true });
    });
  });
}
