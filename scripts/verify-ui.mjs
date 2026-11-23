import { createRequire } from "node:module";
import { resolve } from "node:path";

const require = createRequire(new URL("../frontend/package.json", import.meta.url));
const { chromium, expect } = require("@playwright/test");
import { fileURLToPath } from "node:url";
import { spawn } from "node:child_process";
const root = fileURLToPath(new URL("..", import.meta.url)).replace(/\/$/, "");
const node = process.execPath;
// Абсолютный путь исключает поиск исполняемого файла в каталогах PATH.
const java = resolve(process.env.JAVA_HOME ?? "/usr", "bin", "java");
const servers = [
  spawn(
    java,
    [
      "-jar",
      `${root}/backend/api/target/api-1.0.0-SNAPSHOT.jar`,
      "--server.port=8082",
    ],
    { stdio: "ignore" },
  ),
  spawn(
    node,
    [
      "node_modules/vite/bin/vite.js",
      "--host",
      "127.0.0.1",
      "--port",
      "5175",
      "--strictPort",
    ],
    {
      cwd: `${root}/frontend`,
      env: { ...process.env, API_URL: "http://127.0.0.1:8082" },
      stdio: "ignore",
    },
  ),
];
let browser;
try {
  for (const url of [
    "http://127.0.0.1:8082/api/games/00000000-0000-0000-0000-000000000000",
    "http://127.0.0.1:5175",
  ]) {
    await expect
      .poll(
        async () => {
          try {
            return (await fetch(url)).status;
          } catch {
            return 0;
          }
        },
        { timeout: 30000 },
      )
      .toBeGreaterThan(0);
  }
  browser = await chromium.launch({ headless: true });
  for (const width of [360, 768, 1440]) {
    const page = await browser.newPage({ viewport: { width, height: 1000 } });
    await page.goto("http://127.0.0.1:5175");
    const created = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/games") && response.status() === 201,
    );
    await page.getByRole("button", { name: "Новая игра", exact: true }).click();
    const snapshot = await (await created).json();
    await expect(page.getByRole("status")).toHaveText("Партия идёт");
    expect(snapshot.width).toBe(10);
    expect(snapshot.height).toBe(20);
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    const cell = await page.locator("[data-cell]").first().boundingBox();
    expect(Math.abs(cell.width - cell.height)).toBeLessThan(1);
    await page.screenshot({
      path: `${root}/docs/verification/desktop-game-${width}.png`,
      fullPage: true,
    });
    await page.getByRole("button", { name: "Пауза", exact: true }).click();
    await expect(page.getByRole("status")).toHaveText("Пауза");
    await expect(page.getByRole("button", { name: "Влево", exact: true })).toBeDisabled();
    await page.screenshot({ path: `${root}/docs/verification/paused-default-${width}.png`, fullPage: true });
    await page.getByRole("button", { name: "Продолжить", exact: true }).click();
    await expect(page.getByRole("status")).toHaveText("Партия идёт");
    let pending;
    await page.route("**/commands", (route) => {
      pending = route;
    });
    await expect(page.getByRole("status")).toHaveText("Обновляем поле…");
    await expect(
      page.getByRole("button", { name: "Новая игра", exact: true }),
    ).toBeDisabled();
    await page.screenshot({
      path: `${root}/docs/verification/loading-${width}.png`,
      fullPage: true,
    });
    await pending.abort();
    await expect(page.getByRole("alert")).toBeVisible();
    await page.screenshot({
      path: `${root}/docs/verification/network-error-${width}.png`,
      fullPage: true,
    });
    await page.unroute("**/commands");
    await page.getByRole("button", { name: "Обновить состояние" }).click();
    await expect(page.getByRole("status")).toHaveText("Партия идёт");
    let state = snapshot;
    for (let i = 0; state.status === "RUNNING" && i < 400; i++) {
      const response = await page.request.post(
        `http://127.0.0.1:8082/api/games/${snapshot.gameId}/commands`,
        { data: { command: "DOWN" } },
      );
      expect(response.status()).toBe(200);
      state = (await response.json()).state;
    }
    expect(state.status).toBe("GAME_OVER");
    await expect(page.getByRole("status")).toHaveText("Партия окончена");
    await page.screenshot({
      path: `${root}/docs/verification/finished-${width}.png`,
      fullPage: true,
    });
    await page.close();
    console.log(
      `Проверены 10 × 20, пауза, загрузка, ошибка и завершение на ширине ${width}`,
    );
  }
} finally {
  await browser?.close();
  for (const server of servers) server.kill("SIGTERM");
}
