import { test, expect } from '@playwright/test';
import { advanceFall, clickButton, openGameWithControlledClock } from './game-actions';

test.describe('Игра — очистка строк', () => {
  test('две O, размещённые рядом, удаляют две строки', async ({ page }) => {
    // Arrange
    await openGameWithControlledClock(page);
    await clickButton(page, 'Влево');
    await advanceFall(page, 3);

    // Act
    await clickButton(page, 'Вправо');
    await advanceFall(page, 3);

    // Assert
    await expect(page.getByTestId('cleared-lines')).toHaveText('2');
    await expect(page.locator('[data-cell="settled"]')).toHaveCount(0);
    await expect(page.locator('[data-cell="active"]')).toHaveCount(4);
  });
});
