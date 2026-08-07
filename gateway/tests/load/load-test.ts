process.env.NODE_ENV = 'test';
import autocannon from 'autocannon';
import { buildApp } from '../../src/app';

async function runLoadTest() {
  const app = await buildApp();
  const address = await app.listen({ port: 0, host: '127.0.0.1' });
  console.log(`[LOAD_TEST] Gateway running on ${address}`);

  const result = await autocannon({
    url: `${address}/api/v1/search?q=cyber`,
    connections: 100,
    duration: 10,
    pipelining: 1,
  });

  console.log('\n========================================');
  console.log('       MILESTONE 0.5 LOAD TEST RESULTS   ');
  console.log('========================================');
  console.log(`Requests Total:  ${result.requests.total}`);
  console.log(`Requests/sec:    ${result.requests.average}`);
  console.log(`Latency Avg:     ${result.latency.average} ms`);
  console.log(`Latency P99:     ${result.latency.p99} ms`);
  console.log(`2xx Responses:   ${result['2xx']}`);
  console.log(`Non-2xx Errors:  ${result.non2xx}`);
  console.log('========================================\n');

  await app.close();
  if (result.non2xx > 0) {
    process.exit(1);
  }
}

runLoadTest();
