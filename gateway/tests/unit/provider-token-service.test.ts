import { describe, it, expect, vi } from 'vitest';
import { ProviderTokenService } from '../../src/providers/youtube/ProviderTokenService';
import { MintedPoToken } from '../../src/providers/youtube/poToken/mint';

function makeMint(ttlSeconds = 7200, shouldFail = false) {
  return vi.fn(async (): Promise<MintedPoToken> => {
    if (shouldFail) throw new Error('mint boom');
    return { poToken: 'po-token', visitorData: 'visitor-1', ttlSeconds };
  });
}

describe('ProviderTokenService', () => {
  it('mints lazily on first getToken and serves the cached token afterwards', async () => {
    const mint = makeMint();
    const svc = new ProviderTokenService(mint);
    const t1 = await svc.getToken();
    const t2 = await svc.getToken();
    expect(t1).toEqual(t2);
    expect(mint).toHaveBeenCalledTimes(1);
    expect(t1.expiresAtEpochSeconds).toBeGreaterThan(Math.floor(Date.now() / 1000));
    expect(t1.poToken).toBe('po-token');
    expect(t1.visitorData).toBe('visitor-1');
  });

  it('refreshes when the cached token has expired', async () => {
    const mint = makeMint(1); // 1 second TTL
    const svc = new ProviderTokenService(mint, 0); // no refresh buffer
    await svc.getToken();
    await new Promise((resolve) => setTimeout(resolve, 1100));
    await svc.getToken();
    expect(mint).toHaveBeenCalledTimes(2);
  });

  it('refreshes early when the token is within the refresh buffer', async () => {
    const mint = makeMint(100); // expires in 100s
    const svc = new ProviderTokenService(mint, 200); // buffer bigger than TTL => always refresh
    await svc.getToken();
    await svc.getToken();
    expect(mint).toHaveBeenCalledTimes(2);
  });

  it('shares a single in-flight mint across concurrent callers', async () => {
    let resolveMint!: (value: MintedPoToken) => void;
    const mint = vi.fn(
      () =>
        new Promise<MintedPoToken>((resolve) => {
          resolveMint = resolve;
        })
    );
    const svc = new ProviderTokenService(mint as never);
    const p1 = svc.getToken();
    const p2 = svc.getToken();
    resolveMint({ poToken: 'po-token', visitorData: 'visitor-1', ttlSeconds: 7200 });
    const [t1, t2] = await Promise.all([p1, p2]);
    expect(t1).toEqual(t2);
    expect(mint).toHaveBeenCalledTimes(1);
  });

  it('forceRefresh always produces a fresh token', async () => {
    const mint = makeMint();
    const svc = new ProviderTokenService(mint);
    await svc.getToken();
    await svc.forceRefresh();
    expect(mint).toHaveBeenCalledTimes(2);
  });

  it('propagates mint failures and records the last error in status', async () => {
    const svc = new ProviderTokenService(makeMint(7200, true));
    await expect(svc.getToken()).rejects.toThrow('mint boom');
    const status = svc.getStatus();
    expect(status.lastError).toBe('mint boom');
    expect(status.hasToken).toBe(false);
    expect(status.mintCount).toBe(0);
  });

  it('recovers after a failed mint', async () => {
    const mint = vi
      .fn<() => Promise<MintedPoToken>>()
      .mockRejectedValueOnce(new Error('transient'))
      .mockResolvedValueOnce({ poToken: 'po-token', visitorData: 'visitor-1', ttlSeconds: 7200 });
    const svc = new ProviderTokenService(mint);
    await expect(svc.getToken()).rejects.toThrow('transient');
    const token = await svc.getToken();
    expect(token.poToken).toBe('po-token');
    expect(svc.getStatus().lastError).toBeNull();
  });

  it('getStatus reports token metadata once minted', async () => {
    const svc = new ProviderTokenService(makeMint());
    await svc.getToken();
    const status = svc.getStatus();
    expect(status.enabled).toBe(true);
    expect(status.hasToken).toBe(true);
    expect(status.mintCount).toBe(1);
    expect(status.expiresAtEpochSeconds).toBeTypeOf('number');
    expect(status.lastMintedAtEpochSeconds).toBeTypeOf('number');
  });
});
