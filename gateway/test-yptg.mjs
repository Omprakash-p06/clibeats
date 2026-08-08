import { generate } from 'youtube-po-token-generator';
console.log('generating...');
const t0 = Date.now();
try {
  const { visitorData, poToken } = await generate();
  console.log('ELAPSED_MS:', Date.now() - t0);
  console.log('visitorData:', visitorData);
  console.log('poToken length:', poToken.length);
  console.log('poToken:', poToken.slice(0, 40) + '...');
} catch (e) {
  console.log('ERROR:', e.message);
}
