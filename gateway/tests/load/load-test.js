"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
process.env.NODE_ENV = 'test';
const autocannon_1 = __importDefault(require("autocannon"));
const app_1 = require("../../src/app");
async function runLoadTest() {
    const app = (0, app_1.buildApp)();
    const address = await app.listen({ port: 0, host: '127.0.0.1' });
    console.log(`[LOAD_TEST] Gateway running on ${address}`);
    const result = await (0, autocannon_1.default)({
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
//# sourceMappingURL=load-test.js.map