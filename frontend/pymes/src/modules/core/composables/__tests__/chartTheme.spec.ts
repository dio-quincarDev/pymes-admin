import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { CATEGORICAL_COLOR_KEYS } from '../useChartTheme';

const css = readFileSync(
  fileURLToPath(new URL('../../../../css/app.scss', import.meta.url)),
  'utf8',
);

describe('paleta categórica de los donuts', () => {
  it('resuelve 6 colores distintos para 5 tajadas + Otros', () => {
    const values = CATEGORICAL_COLOR_KEYS.map((key) => {
      const found = css.match(new RegExp(`--pq-${key}:\\s*([^;]+);`));
      expect(found, `--pq-${key} debe estar en app.scss`).not.toBeNull();
      return found?.[1]?.trim();
    });

    expect(new Set(values).size).toBe(6);
  });
});
