import { test, expect } from '@playwright/test';
import { advanceFall, clickButton, openGameWithControlledClock } from './game-actions';

test.describe('Игра — окончание и новая партия', () => {
  test('переполнение стартовой позиции завершает партию и отключает команды', async ({ page }) => {
    // Arrange
    await openGameWithControlledClock(page);

    // Act
    await advanceFall(page, 4);

    // Assert
    await expect(page.getByRole('status')).toHaveText('Партия окончена');
    await expect(page.getByRole('button', { name: 'Повернуть' })).toBeDisabled();
    await expect(page.locator('[data-cell="settled"]')).toHaveCount(8);
  });

  test('новая партия после окончания очищает поле и счётчик', async ({ page }) => {
    // Arrange
    await openGameWithControlledClock(page);
    await advanceFall(page, 4);

    // Act
    await clickButton(page, 'Новая игра');

    // Assert
    await expect(page.getByRole('status')).toHaveText('Партия идёт');
    await expect(page.getByTestId('cleared-lines')).toHaveText('0');
    await expect(page.locator('[data-cell="settled"]')).toHaveCount(0);
  });
});
