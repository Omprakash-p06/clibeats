/**
 * Ambient type declarations for bgutils-js subpath exports.
 *
 * bgutils-js is an ESM-only package (its package.json `exports` map exposes
 * only `./botguard`, `./webpo` and `./utils`). The gateway compiles to CommonJS
 * with classic node10 module resolution, which cannot read package `exports`
 * maps, so we declare the small API surface we use here. At runtime the emitted
 * `require('bgutils-js/botguard')` resolves through the exports map and loads
 * the ESM module via Node's require(esm) support (Node >= 20.19 / 22.x).
 *
 * Keep this file in sync with the upstream declarations under
 * `node_modules/bgutils-js/dist` when upgrading the dependency.
 */
declare module 'bgutils-js/botguard' {
  export interface ContentBiding {
    c?: string;
    e?: string;
    encryptedVideoId?: string;
    externalChannelId?: string;
    commentId?: string;
    atr_challenge?: string;
    [key: string]: unknown;
  }

  export type WebPoSignalOutputFunction = (
    buffer: Uint8Array
  ) => Promise<(contentBinding: Uint8Array) => Promise<Uint8Array | undefined>>;

  export type WebPoSignalOutput = (WebPoSignalOutputFunction | undefined)[];

  export interface SnapshotArgs {
    contentBinding?: ContentBiding;
    signedTimestamp?: unknown;
    webPoSignalOutput?: WebPoSignalOutput;
    skipPrivacyBuffer?: boolean;
  }

  export interface BotGuardClientOptions {
    program?: string;
    globalName?: string;
    globalObject?: any;
    userInteractionElement?: any;
  }

  export class BotGuardClient {
    vm: Record<string, any>;
    program: string;
    static create(options: BotGuardClientOptions): Promise<BotGuardClient>;
    snapshot(args: SnapshotArgs, timeout?: number): Promise<string>;
    passEvent(args: unknown, timeout?: number): Promise<void>;
    shutdown(timeout?: number): Promise<void>;
  }
}

declare module 'bgutils-js/webpo' {
  export interface IntegrityTokenData {
    integrityToken?: string;
    estimatedTtlSecs?: number;
    mintRefreshThreshold?: number;
    websafeFallbackToken?: string;
  }

  export class WebPoMinter {
    static create(
      integrityTokenResponse: IntegrityTokenData,
      webPoSignalOutput: import('bgutils-js/botguard').WebPoSignalOutput
    ): Promise<WebPoMinter>;
    mintAsWebsafeString(contentBinding: string): Promise<string>;
  }
}

declare module 'bgutils-js/utils' {
  export const GOOG_BASE_URL: string;
  export const YT_BASE_URL: string;
  export const GOOG_API_KEY: string;
  export const USER_AGENT: string;
  export function getHeaders(): Record<string, string>;
  export function buildURL(endpointName: string, useYouTubeAPI?: boolean): string;
  export function parseLooseJSON(looseJson: string): Record<string, any>;
  export function base64ToU8(base64: string): Uint8Array;
  export function u8ToBase64(u8: Uint8Array, base64url?: boolean): string;
}
