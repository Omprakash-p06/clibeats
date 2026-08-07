import { ProviderAdapter } from '../../types/adapter';
import { ProviderContext } from '../../types/context';
import { ProviderCapabilities } from '../../types/capabilities';
import { ProviderRegistry } from '../registry/ProviderRegistry';
import { CircuitBreaker } from '../circuit/CircuitBreaker';
import { globalEventBus } from '../events/EventBus';
import { ProviderError, InternalError } from '../../types/error';

export class ProviderSelectionEngine {
  private circuitBreakers: Map<string, CircuitBreaker> = new Map();

  constructor(private registry: ProviderRegistry) {}

  public getCircuitBreaker(providerId: string): CircuitBreaker {
    let cb = this.circuitBreakers.get(providerId);
    if (!cb) {
      cb = new CircuitBreaker(providerId);
      this.circuitBreakers.set(providerId, cb);
    }
    return cb;
  }

  public async computeScore(
    adapter: ProviderAdapter,
    requiredCapability: keyof ProviderCapabilities,
    context: ProviderContext
  ): Promise<number> {
    const cb = this.getCircuitBreaker(adapter.id);
    if (!cb.isAvailable()) return -1000;

    let score = adapter.priority;

    // Check capability
    if (adapter.capabilities[requiredCapability]) {
      score += 50;
    } else {
      return -1000; // Incapable
    }

    // Health check check
    try {
      const health = await adapter.healthCheck();
      score += health.score; // 0 to 100
      if (health.latencyMs < 200) score += 20;
      else if (health.latencyMs > 1000) score -= 20;
    } catch {
      score -= 50;
    }

    return score;
  }

  public async selectBestProvider(
    requiredCapability: keyof ProviderCapabilities,
    context: ProviderContext
  ): Promise<ProviderAdapter> {
    const adapters = this.registry.getAll();
    if (adapters.length === 0) {
      throw new InternalError('No provider adapters registered in engine', 'gateway');
    }

    const scored: Array<{ adapter: ProviderAdapter; score: number }> = [];

    for (const adapter of adapters) {
      const score = await this.computeScore(adapter, requiredCapability, context);
      if (score > 0) {
        scored.push({ adapter, score });
      }
    }

    scored.sort((a, b) => b.score - a.score);

    if (scored.length === 0) {
      throw new InternalError(
        `No available provider supports capability: ${requiredCapability}`,
        'gateway'
      );
    }

    const selected = scored[0].adapter;
    globalEventBus.emitEvent({
      type: 'PROVIDER_SELECTED',
      traceId: context.traceId,
      providerId: selected.id,
      score: scored[0].score,
    });

    return selected;
  }

  public async executeWithFailover<T>(
    requiredCapability: keyof ProviderCapabilities,
    context: ProviderContext,
    operation: (adapter: ProviderAdapter) => Promise<T>
  ): Promise<T> {
    const adapters = this.registry.getAll();
    const candidates: Array<{ adapter: ProviderAdapter; score: number }> = [];

    for (const adapter of adapters) {
      const score = await this.computeScore(adapter, requiredCapability, context);
      if (score > 0) {
        candidates.push({ adapter, score });
      }
    }

    candidates.sort((a, b) => b.score - a.score);

    let lastError: Error | null = null;
    let previousProviderId: string | null = null;

    for (const { adapter } of candidates) {
      const cb = this.getCircuitBreaker(adapter.id);
      try {
        if (previousProviderId) {
          globalEventBus.emitEvent({
            type: 'PROVIDER_FAILOVER',
            traceId: context.traceId,
            fromProvider: previousProviderId,
            toProvider: adapter.id,
          });
        }
        const result = await operation(adapter);
        cb.recordSuccess();
        return result;
      } catch (err: any) {
        cb.recordFailure();
        previousProviderId = adapter.id;
        lastError = err;
        globalEventBus.emitEvent({
          type: 'PROVIDER_FAILED',
          traceId: context.traceId,
          providerId: adapter.id,
          error: err.message || String(err),
        });
      }
    }

    if (lastError instanceof ProviderError) {
      throw lastError;
    }
    throw new InternalError(
      `All provider failover candidates failed for ${requiredCapability}: ${lastError?.message}`,
      'gateway'
    );
  }
}
