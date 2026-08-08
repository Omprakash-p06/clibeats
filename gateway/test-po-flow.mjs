// test-po-flow.mjs
// End-to-end empirical validation of the PO token flow, following the canonical
// bgutils-js example (LuanRT/BgUtils examples/index-innertube.ts):
//   jsdom environment -> interpreter via new Function -> BotGuardClient
//   -> snapshot({ webPoSignalOutput }) -> WAA GenerateIT -> WebPoMinter
//   -> mint -> getBasicInfo with po_token
import { Innertube, ProtoUtils, Platform, ClientType } from 'youtubei.js';
import { BotGuardClient } from 'bgutils-js/botguard';
import { WebPoMinter } from 'bgutils-js/webpo';
import { GOOG_API_KEY } from 'bgutils-js/utils';
import { JSDOM } from 'jsdom';

const VIDEO_ID = 'rj5wZqReXQE'; // Wonderwall
const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36';

async function setupBotGuard(interpreterJavascript, globalName, program) {
  const dom = new JSDOM('<!DOCTYPE html><html lang="en"><head><title></title></head><body></body></html>', {
    url: 'https://www.youtube.com',
    referrer: 'https://www.youtube.com/',
    userAgent: USER_AGENT,
  });
  dom.window.yt = { config_: { EVENT_ID: 'test' } };
  Object.assign(globalThis, {
    window: dom.window,
    document: dom.window.document,
    location: dom.window.location,
    origin: dom.window.origin,
  });
  if (!('navigator' in globalThis)) {
    Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator });
  }
  new Function(interpreterJavascript)();
  if (!globalThis[globalName]) throw new Error('Interpreter did not expose globalName');
  const botGuardClient = await BotGuardClient.create({
    program,
    globalName,
    globalObject: globalThis,
  });
  return { botGuardClient, dom };
}

async function mintPoToken(visitorData) {
  console.log('[1] Creating WEB Innertube session...');
  const yt = await Innertube.create({ client_type: 'WEB', generate_session_locally: true });
  const sessionVisitorData = yt.session.context.client.visitorData;
  console.log('    session visitorData:', sessionVisitorData);

  console.log('[2] Getting attestation challenge (InnerTube)...');
  const challenge = await yt.getAttestationChallenge('ENGAGEMENT_TYPE_UNBOUND');
  const bg = challenge.bg_challenge;
  const requestKey = challenge.challenge;
  console.log('    requestKey length:', requestKey?.length);
  const wrappedUrl = bg.interpreter_url?.private_do_not_access_or_else_trusted_resource_url_wrapped_value;
  if (!wrappedUrl) throw new Error('No interpreter URL');

  console.log('[3] Fetching interpreter JS...');
  const url = wrappedUrl.startsWith('http') ? wrappedUrl : `https:${wrappedUrl}`;
  const resp = await fetch(url);
  if (!resp.ok) throw new Error(`Interpreter fetch failed: ${resp.status}`);
  const interpreterJavascript = await resp.text();
  console.log('    interpreter length:', interpreterJavascript.length);

  console.log('[4] Setting up jsdom environment + BotGuardClient...');
  const { botGuardClient } = await setupBotGuard(interpreterJavascript, bg.global_name, bg.program);
  console.log('    BotGuardClient loaded');

  console.log('[5] snapshot({ webPoSignalOutput })...');
  const webPoSignalOutput = [];
  const botguardResponse = await botGuardClient.snapshot({ webPoSignalOutput });
  console.log('    botguardResponse length:', botguardResponse?.length);
  console.log('    webPoSignalOutput length:', webPoSignalOutput.length);
  if (webPoSignalOutput.length === 0) throw new Error('webPoSignalOutput EMPTY - environment checks failed');

  console.log('[6] WAA GenerateIT...');
  const waaResp = await fetch('https://jnn-pa.googleapis.com/$rpc/google.internal.waa.v1.Waa/GenerateIT', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json+protobuf',
      'x-goog-api-key': GOOG_API_KEY,
      'x-user-agent': 'grpc-web-javascript/0.1',
      'User-Agent': USER_AGENT,
    },
    body: JSON.stringify([requestKey, botguardResponse]),
  });
  console.log('    WAA status:', waaResp.status);
  const waaData = await waaResp.json();
  if (!Array.isArray(waaData) || typeof waaData[0] !== 'string') {
    throw new Error(`WAA failed: ${JSON.stringify(waaData).slice(0, 300)}`);
  }
  const integrityTokenData = {
    integrityToken: waaData[0],
    estimatedTtlSecs: waaData[1],
    mintRefreshThreshold: waaData[2],
    websafeFallbackToken: waaData[3],
  };
  console.log('    integrity token length:', integrityTokenData.integrityToken?.length);

  console.log('[7] Creating WebPoMinter...');
  const minter = await WebPoMinter.create(integrityTokenData, webPoSignalOutput);

  // Mint with both candidate content bindings
  let decoded;
  try { decoded = ProtoUtils.decodeVisitorData(sessionVisitorData); } catch { decoded = null; }
  const visitorId = decoded?.id ?? sessionVisitorData;
  const tokenByVisitorId = await minter.mintAsWebsafeString(visitorId);
  console.log('    token minted w/ visitorId:', visitorId, '-> length', tokenByVisitorId.length);
  return { visitorData: sessionVisitorData, visitorId, tokenByVisitorId };
}

async function testPlayback(poToken, visitorData) {
  for (const c of [{ name: 'ANDROID_VR', type: ClientType.ANDROID_VR }, { name: 'ANDROID', type: ClientType.ANDROID }, { name: 'WEB', type: ClientType.WEB }]) {
    console.log(`\n=== ${c.name} with po_token (visitorId binding) ===`);
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

try {
  const { visitorData, visitorId, tokenByVisitorId } = await mintPoToken();
  console.log('\n==== MINTED ====');
  await testPlayback(tokenByVisitorId, visitorData);
} catch (err) {
  console.error('\nFLOW FAILED:', err.message);
  process.exit(1);
}
console.log('\nDONE');
