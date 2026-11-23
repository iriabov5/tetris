import { mkdtemp, readdir, readFile, rename, rm } from 'node:fs/promises';
import { dirname, join, relative, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { randomUUID } from 'node:crypto';
import { generate } from 'orval';
import { apiConfig, generatedDirectory } from '../orval.config.ts';

const comparePaths = (left, right) => left.localeCompare(right, 'en');

async function files(directory, root = directory) {
  let entries;
  try {
    entries = await readdir(directory, { withFileTypes: true });
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw error;
  }
  const result = [];
  for (const entry of entries) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) result.push(...(await files(path, root)));
    else result.push(relative(root, path));
  }
  return result.sort(comparePaths);
}

export async function differences(expected, actual) {
  const expectedFiles = new Set(await files(expected));
  const actualFiles = new Set(await files(actual));
  const result = [];
  for (const file of [...new Set([...expectedFiles, ...actualFiles])].sort(comparePaths)) {
    if (!expectedFiles.has(file)) result.push(`Лишний: ${file}`);
    else if (!actualFiles.has(file)) result.push(`Отсутствует: ${file}`);
    else if (!(await readFile(join(expected, file))).equals(await readFile(join(actual, file)))) {
      result.push(`Изменён: ${file}`);
    }
  }
  return result;
}

async function publish(staging, target) {
  const backup = `${target}.previous-${randomUUID()}`;
  let previous = false;
  try {
    await rename(target, backup);
    previous = true;
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  try {
    await rename(staging, target);
  } catch (error) {
    if (previous) await rename(backup, target);
    throw error;
  }
  if (previous) await rm(backup, { recursive: true });
}

export async function run(mode, { target = generatedDirectory, input } = {}) {
  if (mode !== 'generate' && mode !== 'check') throw new Error(`Неизвестный режим: ${mode}`);
  // Та же глубина каталогов сохраняет относительные импорты транспорта при сравнении.
  const staging = await mkdtemp(join(dirname(target), '.api-client-'));
  try {
    // Программный API Orval по умолчанию только журналирует ошибку: здесь публикация запрещена.
    await generate(apiConfig(staging, input).tetris, resolve(import.meta.dirname, '..'), {
      throwOnError: true,
      failOnWarnings: true,
    });
    if (mode === 'generate') await publish(staging, target);
    else {
      const changes = await differences(staging, target);
      if (changes.length) {
        throw new Error(
          `SDK расходится с OpenAPI:\n${changes.join('\n')}\nВыполните npm run api:generate`,
        );
      }
    }
  } finally {
    await rm(staging, { recursive: true, force: true });
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  try {
    await run(process.argv[2]);
    console.info('SDK соответствует OpenAPI.');
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
