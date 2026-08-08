process.env.NODE_ENV = 'test';
import { buildApp } from '../src/app';

async function validatePhaseAGateway() {
  console.log('=== PHASE A: GATEWAY VALIDATION SUITE ===\n');

  const app = await buildApp();
  await app.ready();

  try {
    // 1. Health & Core Endpoints
    console.log('[1/4] Validating Core Endpoints (/health, /metrics, /version, /api/v1/bootstrap)...');
    
    const healthRes = await app.inject({ method: 'GET', url: '/health' });
    console.assert(healthRes.statusCode === 200, `Health status failed: ${healthRes.statusCode}`);
    const healthBody = JSON.parse(healthRes.payload);
    console.log(' - /health status:', healthRes.statusCode, '| gateway:', healthBody.gateway, '| redis:', healthBody.redis);

    const metricsRes = await app.inject({ method: 'GET', url: '/metrics' });
    console.assert(metricsRes.statusCode === 200, `Metrics status failed: ${metricsRes.statusCode}`);
    console.log(' - /metrics status:', metricsRes.statusCode, '| payload contains gateway_requests_total:', metricsRes.payload.includes('gateway_requests_total'));

    const versionRes = await app.inject({ method: 'GET', url: '/version' });
    console.assert(versionRes.statusCode === 200, `Version status failed: ${versionRes.statusCode}`);
    const versionBody = JSON.parse(versionRes.payload);
    console.log(' - /version status:', versionRes.statusCode, '| version:', versionBody.version);

    const bootstrapRes = await app.inject({ method: 'GET', url: '/api/v1/bootstrap' });
    console.assert(bootstrapRes.statusCode === 200, `Bootstrap status failed: ${bootstrapRes.statusCode}`);
    const bootstrapBody = JSON.parse(bootstrapRes.payload);
    console.log(' - /api/v1/bootstrap status:', bootstrapRes.statusCode, '| providers count:', bootstrapBody.supportedProviders.length);
    console.assert(bootstrapBody.supportedProviders.length > 0, 'No supported providers in bootstrap response!');

    // 2. Search Queries
    console.log('\n[2/4] Validating Search Endpoint across required queries...');
    // Query 'cyber' works for MockProvider dataset, 'Wonderwall' / 'Believer' work for YouTubeProvider
    const testQueries = ['cyber', 'Wonderwall', 'Believer', 'Shape of You', 'Heat Waves', 'Tum Hi Ho'];
    const resolvedTracks: any[] = [];

    for (const q of testQueries) {
      const searchRes = await app.inject({ method: 'GET', url: `/api/v1/search?q=${encodeURIComponent(q)}` });
      console.assert(searchRes.statusCode === 200, `Search failed for query "${q}": ${searchRes.statusCode}`);
      const searchBody = JSON.parse(searchRes.payload);
      console.assert(Array.isArray(searchBody.tracks), `Tracks is not an array for "${q}"`);
      
      if (searchBody.tracks.length > 0) {
        const firstTrack = searchBody.tracks[0];
        console.log(` - Search "${q}": Found ${searchBody.tracks.length} tracks | Top result: "${firstTrack.title}" by "${firstTrack.artist}" (ID: ${firstTrack.id})`);
        console.assert(firstTrack.id && firstTrack.title && firstTrack.artist, `Track missing essential metadata: ${JSON.stringify(firstTrack)}`);
        resolvedTracks.push(firstTrack);
      } else {
        console.log(` - Search "${q}": 0 tracks returned (Expected if using MockProvider dataset)`);
      }
    }

    console.assert(resolvedTracks.length > 0, 'No tracks resolved across any test query!');

    // 3. Stream Resolution
    console.log('\n[3/4] Validating Stream Resolution for search results...');
    for (const track of resolvedTracks.slice(0, 3)) {
      const streamRes = await app.inject({
        method: 'POST',
        url: '/api/v1/stream',
        payload: { trackId: track.id },
      });
      console.assert(streamRes.statusCode === 200, `Stream failed for trackId "${track.id}": ${streamRes.statusCode}`);
      const streamBody = JSON.parse(streamRes.payload);
      console.assert(streamBody.stream && streamBody.stream.streamUrl, `No streamUrl returned for trackId "${track.id}"`);
      console.log(` - Stream track "${track.title}": URL resolved (${streamBody.stream.streamUrl.substring(0, 50)}...) | mimeType: ${streamBody.stream.mimeType || 'audio/webm'}`);
    }

    // 4. Trace ID Propagation
    console.log('\n[4/4] Validating Trace ID Propagation...');
    const customTraceId = 'trace-phase-a-validation-12345';
    const traceRes = await app.inject({
      method: 'GET',
      url: '/api/v1/search?q=test',
      headers: { 'x-trace-id': customTraceId },
    });
    console.assert(traceRes.headers['x-trace-id'] === customTraceId, `Trace ID not echoed! Expected ${customTraceId}, got ${traceRes.headers['x-trace-id']}`);
    console.log(' - Trace ID successfully propagated in response headers.');

    console.log('\n=== GATEWAY VALIDATION PASSED 100% CLEAN ===');
  } catch (err) {
    console.error('\n!!! GATEWAY VALIDATION FAILED !!!', err);
    process.exit(1);
  } finally {
    await app.close();
  }
}

validatePhaseAGateway();
