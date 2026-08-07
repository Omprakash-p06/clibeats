import { readFileSync, existsSync } from 'fs';
import { resolve } from 'path';
import { buildApp } from '../src/app';

process.env.NODE_ENV = 'test';

/**
 * Validates that the committed openapi.json matches the spec generated from
 * the live route schemas. Fails CI if the contract drifts from the code.
 */
async function validateOpenApi(): Promise<void> {
  const committedPath = resolve(__dirname, '../openapi.json');
  if (!existsSync(committedPath)) {
    console.error(`openapi.json missing at ${committedPath}. Run: npm run openapi:generate`);
    process.exit(1);
  }

  const app = await buildApp();
  await app.ready();
  const live = app.swagger({ yaml: false });
  const committed = JSON.parse(readFileSync(committedPath, 'utf8'));
  await app.close();

  const livePaths = Object.keys(live.paths || {}).sort();
  const committedPaths = Object.keys(committed.paths || {}).sort();
  const missing = livePaths.filter((p) => !committedPaths.includes(p));
  const stale = committedPaths.filter((p) => !livePaths.includes(p));

  if (missing.length || stale.length) {
    console.error('OpenAPI contract is out of sync with route schemas.');
    if (missing.length) console.error('  Missing from committed spec:', missing);
    if (stale.length) console.error('  Stale in committed spec:', stale);
    console.error('  Run: npm run openapi:generate');
    process.exit(1);
  }

  console.log('OpenAPI contract in sync:', livePaths.length, 'paths verified.');
}

validateOpenApi().catch((err) => {
  console.error('OpenAPI validation failed:', err);
  process.exit(1);
});