import { describe, expect, it } from 'vitest';
import fs from 'fs';
import path from 'path';

const SRC = path.resolve(__dirname, '../../src');

function walk(dir: string): string[] {
  const results: string[] = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) results.push(...walk(full));
    else if (entry.name.endsWith('.ts')) results.push(full);
  }
  return results;
}

function readImports(file: string): string[] {
  const content = fs.readFileSync(file, 'utf8');
  const imports: string[] = [];
  const re = /from\s+['"]([^'"]+)['"]/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(content)) !== null) imports.push(m[1]);
  return imports;
}

function relative(file: string): string {
  return path.relative(SRC, file).split(path.sep).join('/');
}

function resolveImport(fromFile: string, target: string): string {
  if (target.startsWith('.')) {
    return path.normalize(path.join(path.dirname(fromFile), target))
      .split(path.sep).join('/');
  }
  return target; // bare/package/module import
}

describe('Architecture Layering (ADR-013)', () => {
  const files = walk(SRC);

it('core never imports from providers (decoupling)', () => {
    const offenders = files
      .filter((f) => relative(f).startsWith('core/'))
      .filter((f) =>
        readImports(f).some((i) => {
          const r = resolveImport(f, i);
          return /\/providers\//.test(r) || r === 'providers';
        })
      )
      .map(relative);
    expect(offenders).toEqual([]);
  });

  it('provider plugins never import from core, config, or app (leaf adapters)', () => {
    const offenders = files
      .filter((f) => relative(f).startsWith('providers/') && !/registerProviders/.test(f))
      .filter((f) =>
        readImports(f).some((i) => {
          const r = resolveImport(f, i);
          return /\/core\//.test(r) || /\/config\//.test(r) || r.endsWith('app');
        })
      )
      .map(relative);
    expect(offenders).toEqual([]);
  });

  it('types layer is a leaf (no internal imports)', () => {
    const offenders = files
      .filter((f) => relative(f).startsWith('types/') && f.endsWith('.ts'))
      .filter((f) =>
        readImports(f).some((i) => {
          const r = resolveImport(f, i);
          return /\/core\//.test(r) || /\/providers\//.test(r) || /\/config\//.test(r) || r.endsWith('app');
        })
      )
      .map(relative);
    expect(offenders).toEqual([]);
  });

  it('config layer depends only on external packages', () => {
    const configFile = path.join(SRC, 'config/config.ts');
    const deps = readImports(configFile);
    expect(deps.every((i) => i.startsWith('.') === false)).toBe(true);
  });

  it('provider plugin bootstrap composes core + concrete adapter', () => {
    const bootstrap = readImports(path.join(SRC, 'providers/registerProviders.ts'));
    const touchesCoreOrConfig = bootstrap.filter((i) => /core|config/.test(i));
    expect(touchesCoreOrConfig.length).toBeGreaterThan(0);
    expect(bootstrap.some((i) => i.includes('MockProviderAdapter'))).toBe(true);
  });
});
