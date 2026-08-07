import { writeFileSync } from 'fs';
import { resolve } from 'path';
import { buildApp } from '../src/app';

process.env.NODE_ENV = 'test';

async function generateOpenApi(): Promise<void> {
  const app = await buildApp();
  await app.ready();
  const spec = app.swagger({ yaml: false });
  const outPath = resolve(__dirname, '../openapi.json');
  writeFileSync(outPath, JSON.stringify(spec, null, 2));
  console.log(`OpenAPI spec written to ${outPath}`);
  const paths = Object.keys(spec.paths || {}).length;
  console.log(`  paths: ${paths}`);
  await app.close();
}

generateOpenApi().catch((err) => {
  console.error('OpenAPI generation failed:', err);
  process.exit(1);
});