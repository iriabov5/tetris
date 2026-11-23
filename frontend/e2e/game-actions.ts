import { expect, type Page } from '@playwright/test';

export async function clickButton(page: Page, name: string) {
  await page.getByRole('button', { name, exact: true }).click();
  await expect(page.getByRole('button', { name: 'Новая игра', exact: true })).toBeEnabled();
}

export async function openGameWithControlledClock(page: Page) {
  await page.clock.install({ time: new Date('2026-01-01T00:00:00Z') });
  await page.goto('/');
  await expect(page.getByRole('button', { name: 'Новая игра' })).toBeVisible();
  await page.clock.pauseAt(new Date('2026-01-01T00:01:00Z'));
  await clickButton(page, 'Новая игра');
  await page.getByLabel('Скорость').selectOption('350');
}

export async function advanceFall(page: Page, steps: number) {
  for (let step = 0; step < steps; step++) {
    const response = page.waitForResponse(
      (result) => result.url().endsWith('/commands') && result.request().method() === 'POST',
    );
    await page.clock.runFor(350);
    await response;
    await expect(page.getByRole('button', { name: 'Новая игра' })).toBeEnabled();
  }
}
