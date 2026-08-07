import type { FastifyInstance } from 'fastify';
import { ProviderRegistry } from '../core/registry/ProviderRegistry';
import { GatewayConfig } from '../config/config';
import { MockProviderAdapter } from './mock/MockProviderAdapter';

export interface ProviderRegistrationOverride {
  mock?: { enabled?: boolean; priority?: number };
}

/**
 * Discovers and registers provider adapters from gateway.yaml config (ADR-013).
 * Provider plugins are config-driven: an adapter is registered only when its
 * `enabled` flag is not explicitly false, mirroring the Milestone 0 default.
 */
export async function registerProviders(
  app: FastifyInstance,
  registry: ProviderRegistry,
  config: GatewayConfig,
  override?: ProviderRegistrationOverride
): Promise<void> {
  const mockConfig = override?.mock ?? config.providers?.mock;
  if (mockConfig == null || mockConfig.enabled !== false) {
    registry.register(new MockProviderAdapter('mock', 42, mockConfig?.priority ?? 100));
  }
  void app;
}