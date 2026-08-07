import client from 'prom-client';
import { globalEventBus, GatewayEventPayload } from '../events/EventBus';

export const register = new client.Registry();
client.collectDefaultMetrics({ register });

export const requestCounter = new client.Counter({
  name: 'gateway_requests_total',
  help: 'Total requests received by endpoint',
  labelNames: ['endpoint'],
});
register.registerMetric(requestCounter);

export const cacheCounter = new client.Counter({
  name: 'gateway_cache_total',
  help: 'Cache hits and misses by namespace',
  labelNames: ['namespace', 'result'],
});
register.registerMetric(cacheCounter);

export const providerSelectionCounter = new client.Counter({
  name: 'gateway_provider_selections_total',
  help: 'Total selections per provider',
  labelNames: ['providerId'],
});
register.registerMetric(providerSelectionCounter);

export const providerFailuresCounter = new client.Counter({
  name: 'gateway_provider_failures_total',
  help: 'Total failures per provider',
  labelNames: ['providerId'],
});
register.registerMetric(providerFailuresCounter);

export const streamResolutionHistogram = new client.Histogram({
  name: 'gateway_stream_resolution_duration_seconds',
  help: 'Duration of stream resolution in seconds',
  buckets: [0.1, 0.25, 0.5, 1, 2.5, 5, 10],
});
register.registerMetric(streamResolutionHistogram);

// Wire EventBus listeners to update Prometheus metrics automatically
globalEventBus.onEvent('*', (event: GatewayEventPayload) => {
  switch (event.type) {
    case 'REQUEST_RECEIVED':
      requestCounter.inc({ endpoint: event.endpoint });
      break;
    case 'CACHE_CHECKED':
      cacheCounter.inc({ namespace: event.namespace, result: event.hit ? 'hit' : 'miss' });
      break;
    case 'PROVIDER_SELECTED':
      providerSelectionCounter.inc({ providerId: event.providerId });
      break;
    case 'PROVIDER_FAILED':
      providerFailuresCounter.inc({ providerId: event.providerId });
      break;
    case 'STREAM_RESOLVED':
      streamResolutionHistogram.observe(event.durationMs / 1000);
      break;
  }
});
