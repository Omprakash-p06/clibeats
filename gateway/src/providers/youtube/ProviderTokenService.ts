/**
 * ProviderTokenService — supplies a fresh YouTube PO token (and the visitor
 * data it is bound to) to the playback path, with automatic refresh.
 *
 * This is the abstraction point required by RECOVERY-10 (Task 4): the rest of
 * the gateway only depends on `getToken()` / `getStatus()`. The actual minting
 * is pluggable (injected function) so the service can be unit-tested without
 * network access and so the strategy can be swapped without touching adapters.
 */
import { MintedPoToken } from './poToken/mint';

export interface ProviderToken {
  poToken: string;
  visitorData: string;
  expiresAtEpochSeconds: number;
}

export type MintFunction = (log?: (message: string) => void) => Promise<MintedPoToken>;

export interface ProviderTokenServiceStatus {
  enabled: boolean;
  hasToken: boolean;
  expiresAtEpochSeconds: number | null;
  lastMintedAtEpochSeconds: number | null;
  mintCount: number;
  lastError: string | null;
}

const DEFAULT_REFRESH_BUFFER_SECONDS = 1800; // refresh 30 minutes before expiry
const DEFAULT_TTL_SECONDS = 7200;

export class ProviderTokenService {
  private token: ProviderToken | null = null;
  private mintInFlight: Promise<ProviderToken> | null = null;
  private lastError: string | null = null;
  private lastMintedAtEpochSeconds: number | null = null;
  private mintCount = 0;

  constructor(
    private readonly mint: MintFunction,
    private readonly refreshBufferSeconds: number = DEFAULT_REFRESH_BUFFER_SECONDS
  ) {}

  /**
   * Returns the current valid token, minting one lazily when none is cached or
   * the cached one is about to expire. Concurrent callers share a single mint.
   */
  async getToken(): Promise<ProviderToken> {
    const nowEpochSeconds = Math.floor(Date.now() / 1000);
    if (
      this.token &&
      this.token.expiresAtEpochSeconds - this.refreshBufferSeconds > nowEpochSeconds
    ) {
      return this.token;
    }
    return this.forceRefresh();
  }

  /**
   * Forces a fresh token (used on playback failure with LOGIN_REQUIRED and on
   * expiry). Concurrent callers share a single mint.
   */
  async forceRefresh(): Promise<ProviderToken> {
    if (this.mintInFlight) return this.mintInFlight;
    this.mintInFlight = this.doMint().finally(() => {
      this.mintInFlight = null;
    });
    return this.mintInFlight;
  }

  getStatus(): ProviderTokenServiceStatus {
    return {
      enabled: true,
      hasToken: this.token !== null,
      expiresAtEpochSeconds: this.token ? this.token.expiresAtEpochSeconds : null,
      lastMintedAtEpochSeconds: this.lastMintedAtEpochSeconds,
      mintCount: this.mintCount,
      lastError: this.lastError,
    };
  }

  private async doMint(): Promise<ProviderToken> {
    try {
      const minted = await this.mint();
      const ttlSeconds =
        Number.isFinite(minted.ttlSeconds) && minted.ttlSeconds > 0
          ? minted.ttlSeconds
          : DEFAULT_TTL_SECONDS;
      const token: ProviderToken = {
        poToken: minted.poToken,
        visitorData: minted.visitorData,
        expiresAtEpochSeconds: Math.floor(Date.now() / 1000) + ttlSeconds,
      };
      this.token = token;
      this.lastError = null;
      this.lastMintedAtEpochSeconds = Math.floor(Date.now() / 1000);
      this.mintCount += 1;
      return token;
    } catch (err) {
      this.lastError = err instanceof Error ? err.message : String(err);
      throw err;
    }
  }
}
