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

export const cacheHitsCounter = new client.Counter({
  name: 'gateway_cache_hits_total',
  help: 'Total cache hits by namespace',
  labelNames: ['namespace'],
});
register.registerMetric(cacheHitsCounter);

export const cacheMissesCounter = new client.Counter({
  name: 'gateway_cache_misses_total',
  help: 'Total cache misses by namespace',
  labelNames: ['namespace'],
});
register.registerMetric(cacheMissesCounter);

export const cacheErrorsCounter = new client.Counter({
  name: 'gateway_cache_errors_total',
  help: 'Total cache operations that failed and degraded fail-open',
  labelNames: ['namespace', 'operation'],
});
register.registerMetric(cacheErrorsCounter);

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

export const providerHealthGauge = new client.Gauge({
  name: 'gateway_provider_health',
  help: 'Current provider health score (0-100)',
  labelNames: ['providerId', 'status'],
});
register.registerMetric(providerHealthGauge);

export const circuitBreakerStateGauge = new client.Gauge({
  name: 'gateway_circuit_breaker_state',
  help: 'Circuit breaker state per provider (0=CLOSED, 1=HALF_OPEN, 2=OPEN)',
  labelNames: ['providerId'],
});
register.registerMetric(circuitBreakerStateGauge);

export const searchLatencyHistogram = new client.Histogram({
  name: 'gateway_search_duration_seconds',
  help: 'Duration of search resolution in seconds',
  labelNames: ['cached'],
  buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5],
});
register.registerMetric(searchLatencyHistogram);

export const streamResolutionHistogram = new client.Histogram({
  name: 'gateway_stream_resolution_duration_seconds',
  help: 'Duration of stream resolution in seconds',
  buckets: [0.1, 0.25, 0.5, 1, 2.5, 5, 10],
});
register.registerMetric(streamResolutionHistogram);

export function recordProviderHealth(providerId: string, status: string, score: number): void {
  providerHealthGauge.set({ providerId, status }, score);
}

export function recordCircuitBreakerState(providerId: string, state: string): void {
  const value = state === 'CLOSED' ? 0 : state === 'HALF_OPEN' ? 1 : 2;
  circuitBreakerStateGauge.set({ providerId }, value);
}

// Wire EventBus listeners to update Prometheus metrics automatically
globalEventBus.onEvent('*', (event: GatewayEventPayload) => {
  switch (event.type) {
    case 'REQUEST_RECEIVED':
      requestCounter.inc({ endpoint: event.endpoint });
      break;
    case 'CACHE_CHECKED':
      if (event.hit) {
        cacheHitsCounter.inc({ namespace: event.namespace });
      } else {
        cacheMissesCounter.inc({ namespace: event.namespace });
      }
      break;
    case 'CACHE_ERROR':
      if ('operation' in event) {
        cacheErrorsCounter.inc({ namespace: event.namespace, operation: event.operation });
      }
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