import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, readdir, rm, unlink, writeFile, mkdir } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { run } from './api-client.mjs';

const root = resolve(import.meta.dirname, '../src/api');

test('Генерация — обнаруживает изменённый, отсутствующий и лишний файл без перезаписи', async () => {
  // Arrange
  const directory = await mkdtemp(join(root, '.codegen-test-'));
  const target = join(directory, 'sdk');
  try {
    await run('generate', { target });
    await run('check', { target });
    const changed = join(target, 'api.gen.ts');
    const missing = join(target, 'models/cell.ts');
    const extra = join(target, 'extra.ts');
    const content = (await readFile(changed, 'utf8')) + '// Changed by test\n';
    await writeFile(changed, content);
    await unlink(missing);
    await writeFile(extra, '// Extra file\n');

    // Act
    await assert.rejects(run('check', { target }), (error) => {
      assert.match(error.message, /Изменён: api.gen.ts/);
      assert.match(error.message, /Отсутствует: models[/\\]cell.ts/);
      assert.match(error.message, /Лишний: extra.ts/);
      assert.match(error.message, /npm run api:generate/);
      return true;
    });

    // Assert
    assert.equal(await readFile(changed, 'utf8'), content);
    await assert.rejects(readFile(missing), { code: 'ENOENT' });
    assert.equal(await readFile(extra, 'utf8'), '// Extra file\n');
    assert.deepEqual(await readdir(directory), ['sdk']);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test('Генерация — сохраняет SDK и удаляет временный каталог при невалидном YAML', async () => {
  // Arrange
  const directory = await mkdtemp(join(root, '.codegen-test-'));
  const target = join(directory, 'sdk');
  const input = join(directory, 'invalid.yaml');
  try {
    await mkdir(target);
    await writeFile(join(target, 'previous.ts'), '// Previous SDK\n');
    await writeFile(input, 'openapi: [');

    // Act
    await assert.rejects(run('generate', { target, input }));

    // Assert
    assert.equal(await readFile(join(target, 'previous.ts'), 'utf8'), '// Previous SDK\n');
    assert.deepEqual((await readdir(directory)).sort(), ['invalid.yaml', 'sdk']);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});
