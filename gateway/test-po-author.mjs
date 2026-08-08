// test-po-author.mjs — verbatim adaptation of LuanRT/BgUtils examples/index-innertube.ts
import { BotGuardClient } from 'bgutils-js/botguard';
import { WebPoMinter } from 'bgutils-js/webpo';
import { buildURL, parseLooseJSON, getHeaders, USER_AGENT } from 'bgutils-js/utils';
import { JSDOM } from 'jsdom';
import Innertube, { Platform } from 'youtubei.js';

Platform.shim.eval = async (data) => new Function(data.output)();

const dom = new JSDOM('<!DOCTYPE html><html lang="en"><head><title></title></head><body></body></html>', {
  url: 'https://www.youtube.com',
  referrer: 'https://www.youtube.com/',
  userAgent: USER_AGENT,
});

const pageResponse = await fetch('https://www.youtube.com', {
  headers: {
    accept: '*/*',
    'accept-language': 'en-US,en;q=0.7',
    'user-agent': USER_AGENT,
  },
});
const pageHtml = await pageResponse.text();
const ytConfig = pageHtml.match(/ytcfg\.set\(({.+?})\);/s)?.[1];
if (!ytConfig) throw new Error('Could not find ytcfg in page HTML');
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
if (!initialAttestationData) throw new Error('Could not find challenge in page HTML');
console.log('ytAtN found');
const initialAttestationDataJson = parseLooseJSON(initialAttestationData[1]);
const challengeResponse = initialAttestationDataJson.R;
console.log('challengeResponse keys:', Object.keys(challengeResponse));
if (!challengeResponse.bgChallenge) throw new Error('Could not get challenge');

const interpreterUrl = challengeResponse.bgChallenge.interpreterUrl.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue;
console.log('interpreterUrl:', interpreterUrl);
const bgScriptResponse = await fetch(`https:${interpreterUrl}`);
const interpreterJavascript = await bgScriptResponse.text();
console.log('interpreter length:', interpreterJavascript.length);
if (interpreterJavascript) {
  new Function(interpreterJavascript)();
} else throw new Error('Could not load VM');

const botGuardClient = await BotGuardClient.create({
  program: challengeResponse.bgChallenge.program,
  globalName: challengeResponse.bgChallenge.globalName,
  globalObject: globalThis,
});
console.log('BotGuardClient loaded');

const requestKey = challengeResponse.challenge ?? 'missing';
console.log('requestKey length:', requestKey.length);
const webPoSignalOutput = [];
const botguardResponse = await botGuardClient.snapshot({ webPoSignalOutput });
console.log('botguardResponse length:', botguardResponse?.length);
console.log('webPoSignalOutput length:', webPoSignalOutput.length);
if (webPoSignalOutput.length === 0) {
  console.log('PMD FAIL — output empty');
  process.exit(1);
}

const payload = [requestKey, botguardResponse];
const integrityTokenResponse = await fetch(buildURL('GenerateIT', true), {
  method: 'POST',
  headers: getHeaders(),
  body: JSON.stringify(payload),
});
const integrityTokenJson = await integrityTokenResponse.json();
console.log('WAA status:', integrityTokenResponse.status);
if (!Array.isArray(integrityTokenJson) || typeof integrityTokenJson[0] !== 'string') {
  console.log('WAA failed:', JSON.stringify(integrityTokenJson).slice(0, 300));
  process.exit(1);
}
const [integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken] = integrityTokenJson;
const integrityTokenData = { integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken };
const webPoMinter = await WebPoMinter.create(integrityTokenData, webPoSignalOutput);
console.log('WebPoMinter created');

// Mint bound to visitor id from the page's visitor data
const visitorId = pageHtml.match(/"visitorData":"([^"]+)/)?.[1];
console.log('page visitorData:', visitorId);
const poToken = await webPoMinter.mintAsWebsafeString(visitorId);
console.log('PO TOKEN length:', poToken.length);
console.log('PO TOKEN:', poToken.slice(0, 40) + '...');
console.log('SUCCESS');
process.exit(0);
