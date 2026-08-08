/**
 * Server-side YouTube PO token (Proof of Origin) minting.
 *
 * Implements the canonical BotGuard/WebPoMinter flow used across the
 * YouTube.js ecosystem (see LuanRT/BgUtils examples/index-innertube.ts):
 *
 *   1. Fetch the YouTube web page to obtain a *real* visitor data string
 *      (a token must be bound to a YouTube-issued visitor data that carries
 *      tracking context; random locally-generated visitor data is rejected).
 *   2. Obtain the BotGuard challenge — prefer the challenge embedded in the
 *      page (`window.ytAtN`), fall back to the InnerTube attestation endpoint.
 *   3. Load the BotGuard interpreter into a browser-like jsdom environment.
 *   4. Run a BotGuard snapshot to obtain the WebPO signal output.
 *   5. Exchange the BotGuard response for an integrity token at Google's
 *      Web Anti-Abuse (WAA) GenerateIT endpoint.
 *   6. Mint a PO token bound to the visitor data.
 *
 * The resulting { poToken, visitorData } pair lets playback requests from
 * datacenter IPs (e.g. Render) pass YouTube's "Sign in to confirm you are not
 * a bot" gate. See .planning/debug/yt-po-token-investigation.md.
 *
 * Type declarations for the ESM-only bgutils-js subpath imports are provided
 * in ./bgutils.d.ts (the emitted require() calls resolve via Node's
 * require(esm) support on Node >= 20.19 / 22.x).
 */

import { BotGuardClient } from 'bgutils-js/botguard';
import { WebPoMinter } from 'bgutils-js/webpo';
import { buildURL, getHeaders, parseLooseJSON, USER_AGENT } from 'bgutils-js/utils';
import { JSDOM } from 'jsdom';
import Innertube, { ClientType, Platform, Types } from 'youtubei.js';

// youtubei.js evaluates generated scripts (e.g. signature deciphering) through
// this shim. Required when any client needs the player to be built locally.
Platform.shim.eval = async (data: Types.BuildScriptResult) => new Function(data.output)();

export interface MintedPoToken {
  poToken: string;
  visitorData: string;
  ttlSeconds: number;
}

export interface MintOptions {
  /** Optional diagnostic logger. */
  log?: (message: string) => void;
  /** Per-fetch timeout in milliseconds. */
  fetchTimeoutMs?: number;
}

const DEFAULT_FETCH_TIMEOUT_MS = 20_000;
const PAGE_URL = 'https://www.youtube.com';
const HOME_HTML = '<!DOCTYPE html><html lang="en"><head><title></title></head><body></body></html>';

interface ChallengeData {
  requestKey: string;
  interpreterUrl: string;
  program: string;
  globalName: string;
}

interface PageData {
  html: string;
  visitorData: string;
}

const noop = (): void => undefined;

async function fetchText(url: string, init: RequestInit = {}, timeoutMs: number): Promise<string> {
  const res = await fetch(url, { ...init, signal: AbortSignal.timeout(timeoutMs) });
  if (!res.ok) throw new Error(`GET ${url} failed with HTTP ${res.status}`);
  return res.text();
}

/** Fetch the YouTube homepage and extract the real visitor data string. */
async function fetchPageData(timeoutMs: number): Promise<PageData> {
  const html = await fetchText(
    PAGE_URL,
    { headers: { accept: '*/*', 'accept-language': 'en-US,en;q=0.7', 'user-agent': USER_AGENT } },
    timeoutMs
  );
  const visitorData = html.match(/"visitorData":"([^"]+)/)?.[1];
  if (!visitorData) {
    throw new Error('Could not extract visitorData from YouTube page (bot-walled or layout changed)');
  }
  return { html, visitorData };
}

/** Extract the BotGuard challenge embedded in the page (`window.ytAtN`). */
function challengeFromPageHtml(html: string, log: (msg: string) => void): ChallengeData | null {
  const ytAtN = html.match(/window\.ytAtN\(\s*({[\s\S]*?})\s*\)/);
  if (!ytAtN) return null;
  try {
    // The page embeds the challenge as loose JSON (unquoted keys), so parse it
    // leniently (same helper as the canonical bgutils-js example).
    const parsed = parseLooseJSON(ytAtN[1]) as {
      R?: {
        challenge?: string;
        bgChallenge?: {
          interpreterUrl?: { privateDoNotAccessOrElseTrustedResourceUrlWrappedValue?: string };
          program?: string;
          globalName?: string;
        };
      };
    };
    const R = parsed?.R;
    if (!R?.challenge || !R?.bgChallenge) return null;
    const bg = R.bgChallenge;
    const wrapped = bg.interpreterUrl?.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue;
    if (!wrapped || !bg.program || !bg.globalName) return null;
    return { requestKey: R.challenge, interpreterUrl: wrapped, program: bg.program, globalName: bg.globalName };
  } catch (err) {
    log(`page challenge parse failed: ${err instanceof Error ? err.message : String(err)}`);
    return null;
  }
}

/** Fetch the BotGuard challenge from the InnerTube attestation endpoint. */
async function challengeFromInnerTube(timeoutMs: number): Promise<ChallengeData> {
  const yt = await Innertube.create({ client_type: ClientType.WEB, generate_session_locally: true });
  const response = await yt.getAttestationChallenge('ENGAGEMENT_TYPE_UNBOUND');
  const bg = response.bg_challenge;
  if (!bg || !response.challenge) throw new Error('InnerTube did not return an attestation challenge');
  const wrapped = bg.interpreter_url?.private_do_not_access_or_else_trusted_resource_url_wrapped_value;
  if (!wrapped || !bg.program || !bg.global_name) {
    throw new Error('InnerTube attestation challenge was incomplete');
  }
  return { requestKey: response.challenge, interpreterUrl: wrapped, program: bg.program, globalName: bg.global_name };
}

/**
 * Mint a PO token bound to a fresh YouTube-issued visitor data.
 *
 * Returns the token, the visitor data it is bound to, and the TTL reported by
 * Google (used to schedule refreshes).
 */
export async function mintPoTokenPair(options: MintOptions = {}): Promise<MintedPoToken> {
  const log = options.log ?? noop;
  const timeoutMs = options.fetchTimeoutMs ?? DEFAULT_FETCH_TIMEOUT_MS;

  // 1. Real visitor data (required content binding for playback PO tokens).
  const page = await fetchPageData(timeoutMs);
  log(`page visitor data acquired (${page.visitorData.length} chars)`);

  // 2. BotGuard challenge (page first, InnerTube fallback).
  const challenge = challengeFromPageHtml(page.html, log) ?? (await challengeFromInnerTube(timeoutMs));
  log(`attestation challenge acquired (requestKey ${challenge.requestKey.length} chars)`);

  // 3. Browser-like environment (canonical jsdom setup from bgutils-js docs).
  const dom = new JSDOM(HOME_HTML, {
    url: PAGE_URL,
    referrer: `${PAGE_URL}/`,
    resources: { userAgent: USER_AGENT },
  });
  const ytConfig = page.html.match(/ytcfg\.set\(({.+?})\);/s)?.[1];
  (dom.window as any).yt = { config_: ytConfig ? JSON.parse(ytConfig) : { EVENT_ID: 'PO_TOKEN_MINT' } };
  Object.assign(globalThis, {
    yt: (dom.window as any).yt,
    window: dom.window,
    document: dom.window.document,
    location: dom.window.location,
    origin: dom.window.origin,
  });
  if (!('navigator' in globalThis)) {
    Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator });
  }

  // 4. Load the BotGuard interpreter and run a snapshot to obtain the minter.
  const interpreterJavascript = await fetchText(`https:${challenge.interpreterUrl}`, {}, timeoutMs);
  log(`BotGuard interpreter loaded (${interpreterJavascript.length} bytes)`);
  new Function(interpreterJavascript)();
  if (!(globalThis as any)[challenge.globalName]) {
    throw new Error('BotGuard interpreter did not initialize (globalName missing)');
  }
  const botGuardClient = await BotGuardClient.create({
    program: challenge.program,
    globalName: challenge.globalName,
    globalObject: globalThis,
  });
  const webPoSignalOutput: import('bgutils-js/botguard').WebPoSignalOutput = [];
  const botguardResponse = await botGuardClient.snapshot({ webPoSignalOutput });
  if (webPoSignalOutput.length === 0) {
    throw new Error('BotGuard produced no WebPO signal output (environment checks failed)');
  }
  log(`BotGuard snapshot resolved (${botguardResponse.length} chars)`);

  // 5. Exchange the response for an integrity token at the WAA endpoint.
  const integrityTokenResponse = await fetch(buildURL('GenerateIT', true), {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify([challenge.requestKey, botguardResponse]),
    signal: AbortSignal.timeout(timeoutMs),
  });
  if (!integrityTokenResponse.ok) {
    throw new Error(`WAA GenerateIT failed with HTTP ${integrityTokenResponse.status}`);
  }
  const integrityTokenJson = (await integrityTokenResponse.json()) as [string, number, number, string];
  if (!Array.isArray(integrityTokenJson) || typeof integrityTokenJson[0] !== 'string') {
    throw new Error('WAA GenerateIT returned no integrity token');
  }
  const [integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken] = integrityTokenJson;

  // 6. Mint a token bound to the visitor data.
  const minter = await WebPoMinter.create(
    { integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken },
    webPoSignalOutput
  );
  const poToken = await minter.mintAsWebsafeString(page.visitorData);
  if (!poToken) throw new Error('PO token minting returned an empty token');
  const ttlSeconds = Number.isFinite(estimatedTtlSecs) && estimatedTtlSecs > 0 ? estimatedTtlSecs : 7200;
  log(`PO token minted (${poToken.length} chars, ttl ${ttlSeconds}s)`);
  return { poToken, visitorData: page.visitorData, ttlSeconds };
}
