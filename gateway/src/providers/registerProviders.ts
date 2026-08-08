import type { FastifyInstance } from 'fastify';
import { ProviderRegistry } from '../core/registry/ProviderRegistry';
import { GatewayConfig } from '../config/config';
import { MockProviderAdapter } from './mock/MockProviderAdapter';
import { YouTubeProviderAdapter } from './youtube/YouTubeProviderAdapter';

export interface ProviderRegistrationOverride {
  mock?: { enabled?: boolean; priority?: number };
  youtube?: { enabled?: boolean; priority?: number };
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

  const youtubeConfig = override?.youtube ?? config.providers?.youtube;
  if (youtubeConfig != null && youtubeConfig.enabled === true) {
    registry.register(new YouTubeProviderAdapter(youtubeConfig.priority ?? 60));
  }
  void app;
}