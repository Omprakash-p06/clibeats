import type { FastifyInstance } from 'fastify';
import { ProviderRegistry } from '../core/registry/ProviderRegistry';
import { GatewayConfig } from '../config/config';
import { logger } from '../core/logging/logger';
import { MockProviderAdapter } from './mock/MockProviderAdapter';
import { YouTubeProviderAdapter } from './youtube/YouTubeProviderAdapter';
import { ProviderTokenService } from './youtube/ProviderTokenService';
import { mintPoTokenPair } from './youtube/poToken/mint';

export interface ProviderRegistrationOverride {
  mock?: { enabled?: boolean; priority?: number };
  youtube?: {
    enabled?: boolean;
    priority?: number;
    poToken?: { enabled?: boolean; refreshBufferSeconds?: number };
  };
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
    const poTokenConfig = youtubeConfig.poToken ?? { enabled: true };
    let tokenService: ProviderTokenService | undefined;
    // Disabled under tests (NODE_ENV=test) so suites stay hermetic — the
    // minting flow performs real BotGuard/WAA network calls and jsdom setup.
    if (poTokenConfig.enabled !== false && process.env.NODE_ENV !== 'test') {
      tokenService = new ProviderTokenService(
        (log) =>
          mintPoTokenPair({
            log: (message) => logger.info({ providerId: 'youtube' }, `po-token: ${message}`),
          }),
        poTokenConfig.refreshBufferSeconds
      );
      // Warm the token in the background so the first stream request is fast;
      // failures are non-fatal and retried lazily on demand.
      tokenService
        .getToken()
        .catch((err: unknown) =>
          logger.warn({ error: err instanceof Error ? err.message : String(err) }, 'po-token warm-up failed')
        );
    }
    registry.register(new YouTubeProviderAdapter(youtubeConfig.priority ?? 60, { tokenService }));
  }
  void app;
}