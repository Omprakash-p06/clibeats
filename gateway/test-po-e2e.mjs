// test-po-e2e.mjs — full E2E: mint PO token (page-based flow) -> getBasicInfo on multiple clients with po_token
import { BotGuardClient } from 'bgutils-js/botguard';
import { WebPoMinter } from 'bgutils-js/webpo';
import { buildURL, parseLooseJSON, getHeaders, USER_AGENT } from 'bgutils-js/utils';
import { JSDOM } from 'jsdom';
import Innertube, { Platform } from 'youtubei.js';

Platform.shim.eval = async (data) => new Function(data.output)();
const VIDEO_ID = 'rj5wZqReXQE'; // Wonderwall

async function mintToken(binding) {
  const dom = new JSDOM('<!DOCTYPE html><html lang="en"><head><title></title></head><body></body></html>', {
    url: 'https://www.youtube.com',
    referrer: 'https://www.youtube.com/',
    userAgent: USER_AGENT,
  });
  const pageResponse = await fetch('https://www.youtube.com', {
    headers: { accept: '*/*', 'accept-language': 'en-US,en;q=0.7', 'user-agent': USER_AGENT },
  });
  const pageHtml = await pageResponse.text();
  const ytConfig = pageHtml.match(/ytcfg\.set\(({.+?})\);/s)?.[1];
  if (!ytConfig) throw new Error('No ytcfg');
  dom.window.yt = { config_: JSON.parse(ytConfig) };
  Object.assign(globalThis, {
    yt: dom.window.yt,
    window: dom.window,
    document: dom.window.document,
    location: dom.window.location,
    origin: dom.window.origin,
  });
  if (!('navigator' in globalThis)) {
    Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator });
  }
  const initialAttestationData = pageHtml.match(/window\.ytAtN\(\s*({[\s\S]*?})\s*\)/);
  if (!initialAttestationData) throw new Error('No ytAtN');
  const challengeResponse = parseLooseJSON(initialAttestationData[1]).R;
  const interpreterUrl = challengeResponse.bgChallenge.interpreterUrl.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue;
  const interpreterJavascript = await (await fetch(`https:${interpreterUrl}`)).text();
  new Function(interpreterJavascript)();
  const botGuardClient = await BotGuardClient.create({
    program: challengeResponse.bgChallenge.program,
    globalName: challengeResponse.bgChallenge.globalName,
    globalObject: globalThis,
  });
  const requestKey = challengeResponse.challenge;
  const webPoSignalOutput = [];
  const botguardResponse = await botGuardClient.snapshot({ webPoSignalOutput });
  if (webPoSignalOutput.length === 0) throw new Error('webPoSignalOutput empty');
  const payload = [requestKey, botguardResponse];
  const integrityTokenResponse = await fetch(buildURL('GenerateIT', true), {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(payload),
  });
  const integrityTokenJson = await integrityTokenResponse.json();
  const [integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken] = integrityTokenJson;
  const webPoMinter = await WebPoMinter.create({ integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken }, webPoSignalOutput);
  const poToken = await webPoMinter.mintAsWebsafeString(binding);
  return { poToken, pageHtml };
}

async function testPlayback(poToken, visitorData) {
  for (const c of [{ name: 'ANDROID_VR', type: 'ANDROID_VR' }, { name: 'ANDROID', type: 'ANDROID' }, { name: 'WEB', type: 'WEB' }, { name: 'MUSIC', type: 'MUSIC' }]) {
    console.log(`\n=== ${c.name} ===`);
    try {
      const yt = await Innertube.create({ client_type: c.type, visitor_data: visitorData, po_token: poToken });
      const info = await yt.getBasicInfo(VIDEO_ID, { po_token: poToken });
      const sd = info.streaming_data;
      const audio = (sd?.adaptive_formats ?? []).filter((f) => String(f.mime_type ?? '').startsWith('audio/'));
      console.log('  status:', JSON.stringify(info.playability_status));
      console.log('  formats:', sd?.adaptive_formats?.length ?? 0, 'audio:', audio.length);
      if (audio.length === 0) continue;
      audio.sort((a, b) => (b.average_bitrate ?? 0) - (a.average_bitrate ?? 0));
      const fmt = audio[0];
      console.log('  top format:', { itag: fmt.itag, mime: fmt.mime_type, hasUrl: !!fmt.url });
      if (!fmt.url) continue;
      for (const [label, range] of [['range 0-0', 'bytes=0-0'], ['range 0-2097151', 'bytes=0-2097151'], ['no-range', undefined]]) {
        const headers = { 'User-Agent': 'com.google.android.apps.youtube.vr/1.0.0 (Android 13)' };
        if (range) headers['Range'] = range;
        const r = await fetch(fmt.url, { method: 'GET', headers, redirect: 'follow' });
        console.log(`  ${label}: HTTP ${r.status} CL=${r.headers.get('content-length')} CR=${r.headers.get('content-range')}`);
        await r.body?.cancel();
      }
    } catch (err) {
      console.log('  ERROR:', err.message);
    }
  }
}

// Option A: token bound to page visitorData string
console.log('==== Minting token bound to page visitorData ====');
const pageVisitorData = (await (await fetch('https://www.youtube.com', { headers: { 'user-agent': USER_AGENT } })).text()).match(/"visitorData":"([^"]+)/)?.[1];
console.log('page visitorData:', pageVisitorData);
const { poToken } = await mintToken(pageVisitorData);
console.log('PO token length:', poToken.length);
await testPlayback(poToken, pageVisitorData);
console.log('\nDONE');
