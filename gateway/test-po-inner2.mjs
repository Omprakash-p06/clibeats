// test-po-inner2.mjs — page visitorData binding + InnerTube challenge: does playback succeed?
import { BotGuardClient } from 'bgutils-js/botguard';
import { WebPoMinter } from 'bgutils-js/webpo';
import { buildURL, getHeaders, USER_AGENT } from 'bgutils-js/utils';
import { JSDOM } from 'jsdom';
import Innertube, { Platform } from 'youtubei.js';

Platform.shim.eval = async (data) => new Function(data.output)();
const VIDEO_ID = 'rj5wZqReXQE';

// Get real page visitorData
const pageHtml = await (await fetch('https://www.youtube.com', { headers: { 'user-agent': USER_AGENT } })).text();
const visitorData = pageHtml.match(/"visitorData":"([^"]+)/)?.[1];
console.log('page visitorData length:', visitorData.length);

console.log('[1] WEB session + InnerTube challenge...');
const yt = await Innertube.create({ client_type: 'WEB', visitor_data: visitorData, generate_session_locally: true });
const challenge = await yt.getAttestationChallenge('ENGAGEMENT_TYPE_UNBOUND');
const bg = challenge.bg_challenge;
const requestKey = challenge.challenge;
console.log('requestKey length:', requestKey.length);

console.log('[2] jsdom env + interpreter + BotGuardClient...');
const dom = new JSDOM('<!DOCTYPE html><html lang="en"><head><title></title></head><body></body></html>', {
  url: 'https://www.youtube.com',
  referrer: 'https://www.youtube.com/',
  userAgent: USER_AGENT,
});
dom.window.yt = { config_: { EVENT_ID: 'TEST_EVENT_ID_1234567890ABCDEF' } };
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
const wrappedUrl = bg.interpreter_url.private_do_not_access_or_else_trusted_resource_url_wrapped_value;
const interpreterJavascript = await (await fetch(`https:${wrappedUrl}`)).text();
new Function(interpreterJavascript)();
const botGuardClient = await BotGuardClient.create({ program: bg.program, globalName: bg.global_name, globalObject: globalThis });
const webPoSignalOutput = [];
const botguardResponse = await botGuardClient.snapshot({ webPoSignalOutput });
console.log('webPoSignalOutput:', webPoSignalOutput.length);
if (webPoSignalOutput.length === 0) process.exit(1);

console.log('[3] WAA GenerateIT...');
const integrityTokenResponse = await fetch(buildURL('GenerateIT', true), {
  method: 'POST',
  headers: getHeaders(),
  body: JSON.stringify([requestKey, botguardResponse]),
});
const integrityTokenJson = await integrityTokenResponse.json();
console.log('WAA status:', integrityTokenResponse.status);
const [integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken] = integrityTokenJson;
const webPoMinter = await WebPoMinter.create({ integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken }, webPoSignalOutput);
const poToken = await webPoMinter.mintAsWebsafeString(visitorData);
console.log('PO token length:', poToken.length);

console.log('[4] Playback test ANDROID_VR...');
const yt2 = await Innertube.create({ client_type: 'ANDROID_VR', visitor_data: visitorData, po_token: poToken });
const info = await yt2.getBasicInfo(VIDEO_ID, { po_token: poToken });
const sd = info.streaming_data;
const audio = (sd?.adaptive_formats ?? []).filter((f) => String(f.mime_type ?? '').startsWith('audio/'));
console.log('status:', JSON.stringify(info.playability_status));
console.log('audio formats:', audio.length);
if (audio.length > 0) {
  audio.sort((a, b) => (b.average_bitrate ?? 0) - (a.average_bitrate ?? 0));
  const fmt = audio[0];
  console.log('top:', { itag: fmt.itag, mime: fmt.mime_type, hasUrl: !!fmt.url });
  if (fmt.url) {
    const r = await fetch(fmt.url, { method: 'GET', headers: { 'User-Agent': 'com.google.android.apps.youtube.vr/1.0.0 (Android 13)', Range: 'bytes=0-2097151' }, redirect: 'follow' });
    console.log('URL probe range: HTTP', r.status, 'CR:', r.headers.get('content-range'));
    await r.body?.cancel();
  }
}
console.log('DONE');
process.exit(0);
