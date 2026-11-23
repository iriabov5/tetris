import { resolve } from 'node:path';
import type { Config } from 'orval';

export const generatedDirectory = resolve(import.meta.dirname, 'src/api/generated');

export function apiConfig(
  outputDirectory = generatedDirectory,
  input = resolve(import.meta.dirname, '../docs/openapi.yaml'),
): Config {
  return {
    tetris: {
      input: { target: input },
      output: {
        target: resolve(outputDirectory, 'api.gen.ts'),
        schemas: resolve(outputDirectory, 'models'),
        client: 'fetch',
        formatter: 'prettier',
        override: {
          useReadonlyArrays: true,
          enumGenerationType: 'union',
          mutator: { path: resolve(import.meta.dirname, 'src/api/http.ts'), name: 'request' },
          fetch: { includeHttpResponseReturnType: false, forceSuccessResponse: true },
        },
      },
    },
  };
}

export default apiConfig();
