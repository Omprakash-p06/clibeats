import { Innertube, ClientType, UniversalCache } from 'youtubei.js';
import { BotGuardClient } from 'bgutils-js/botguard';
import { WebPoMinter } from 'bgutils-js/webpo';
import vm from 'node:vm';

async function generatePoToken() {
  console.log('[1] Creating Innertube session (WEB)...');
  const yt = await Innertube.create({
    client_type: ClientType.WEB,
    generate_session_locally: true,
  });

  console.log('[2] Getting attestation challenge...');
  const challengeResponse = await yt.getAttestationChallenge('ENGAGEMENT_TYPE_UNBOUND');
  const bgChallenge = challengeResponse.bg_challenge;
  const innerTubeChallenge = challengeResponse.challenge;

  console.log('bg_challenge keys:', Object.keys(bgChallenge));
  console.log('interpreter_hash:', bgChallenge.interpreter_hash);
  console.log('global_name:', bgChallenge.global_name);
  console.log('innerTube challenge:', innerTubeChallenge);

  // The InnerTube challenge string IS the request key for the WAA API
  const requestKey = innerTubeChallenge;
  console.log('Request key (InnerTube challenge):', requestKey);

  console.log('[3] Fetching and executing BotGuard interpreter JS...');
  const interpreterUrl = 'https:' + bgChallenge.interpreter_url?.private_do_not_access_or_else_trusted_resource_url_wrapped_value;
  console.log('Interpreter URL:', interpreterUrl);

  const resp = await fetch(interpreterUrl);
  const interpreterJs = await resp.text();
  console.log('Interpreter JS length:', interpreterJs.length);

  // Create VM context and execute interpreter
  const context = vm.createContext({
    window: {},
    document: {
      createElement: () => ({ style: {}, setAttribute: () => {}, appendChild: () => {} }),
      addEventListener: () => {},
    },
    navigator: { userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36' },
    setTimeout: setTimeout,
    clearTimeout: clearTimeout,
    console: { log: () => {}, error: () => {}, warn: () => {} },
    Math,
    JSON,
    atob,
    btoa,
    Uint8Array,
    TextDecoder,
    TextEncoder,
  });

  vm.runInContext(interpreterJs, context);
  const globalObj = context;

  console.log('[4] Creating BotGuardClient...');
  const botguard = await BotGuardClient.create({
    globalObject: globalObj,
    globalName: bgChallenge.global_name,
    program: bgChallenge.program,
  });

  console.log('[5] Taking snapshot with webPoSignalOutput...');
  const webPoSignalOutput = [];

  const botguardResponse = await botguard.snapshot({
    webPoSignalOutput,
    contentBinding: {
      c: innerTubeChallenge,
    },
  });

  console.log('BotGuard response length:', botguardResponse?.length || 'undefined');
  console.log('webPoSignalOutput length:', webPoSignalOutput.length);
  console.log('webPoSignalOutput[0] type:', typeof webPoSignalOutput[0]);

  console.log('[6] Requesting integrity token from WAA API...');

  // Try using YouTube API endpoint (useYouTubeAPI=true)
  const payload = [requestKey, botguardResponse];

  // Try the YouTube API endpoint first
  const waaUrl = 'https://www.youtube.com/api/jnn/v1/GenerateIT';
  console.log('Trying YouTube API endpoint:', waaUrl);

  const waaResp = await fetch(waaUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json+protobuf',
      'x-goog-api-key': 'AIzaSyDyT5W0Jh49F30Ppqtyfdf7pDLFKLJoAnw',
      'x-user-agent': 'grpc-web-javascript/0.1',
      'User-Agent': 'Mozilla/5.0',
    },
    body: JSON.stringify(payload),
  });

  console.log('WAA response status:', waaResp.status);
  const waaData = await waaResp.json();

  if (!Array.isArray(waaData) || typeof waaData[0] !== 'string') {
    console.log('WAA YouTube API response (full):', JSON.stringify(waaData).slice(0, 500));
    console.log('Retrying with direct WAA endpoint...');

    // Try the direct WAA endpoint
    const waaUrl2 = 'https://jnn-pa.googleapis.com/$rpc/google.internal.waa.v1.Waa/GenerateIT';
    const waaResp2 = await fetch(waaUrl2, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json+protobuf',
        'x-goog-api-key': 'AIzaSyDyT5W0Jh49F30Ppqtyfdf7pDLFKLJoAnw',
        'x-user-agent': 'grpc-web-javascript/0.1',
        'User-Agent': 'Mozilla/5.0',
      },
      body: JSON.stringify(payload),
    });
    const waaData2 = await waaResp2.json();
    console.log('WAA direct response status:', waaResp2.status);
    console.log('WAA direct response (full):', JSON.stringify(waaData2).slice(0, 500));
  }

  if (Array.isArray(waaData) && typeof waaData[0] === 'string') {
    const integrityToken = waaData[0];
    const estimatedTtlSecs = waaData[1];
    const mintRefreshThreshold = waaData[2];
    const websafeFallbackToken = waaData[3];

    console.log('\n[7] Creating WebPoMinter and minting PO token...');
    const minter = await WebPoMinter.create(
      { integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken },
      webPoSignalOutput
    );

    // Use a visitor ID as content binding
    const visitorId = '6zpwvWUNAco';
    const poToken = await minter.mintAsWebsafeString(visitorId);

    console.log('PO TOKEN:', poToken);
    console.log('Token length:', poToken?.length);

    console.log('\n[8] Testing PO token with ANDROID_VR client...');
    const yt2 = await Innertube.create({
      client_type: ClientType.ANDROID_VR,
      generate_session_locally: true,
    });

    const info = await yt2.getBasicInfo('rj5wZqReXQE', { po_token: poToken });
    console.log('Playability status:', JSON.stringify(info.playability_status));
    const sd = info.streaming_data;
    const audio = (sd?.adaptive_formats ?? []).filter((f) => String(f.mime_type ?? '').startsWith('audio/'));
    console.log('Formats count:', sd?.adaptive_formats?.length ?? 0);
    console.log('Audio formats count:', audio.length);

    if (audio[0]?.url) {
      console.log('First audio format:', {
        itag: audio[0].itag,
        mime_type: audio[0].mime_type,
        bitrate: audio[0].average_bitrate,
        hasUrl: true,
      });

      // Test HTTP status of the URL
      console.log('\n[9] Testing HTTP status of stream URL...');
      const probeResp = await fetch(audio[0].url, {
        method: 'HEAD',
        headers: {
          'User-Agent': 'com.google.android.apps.youtube.vr/1.0.0',
          Range: 'bytes=0-0',
        },
        redirect: 'follow',
      });
      console.log('HEAD response status:', probeResp.status);
      console.log('Content-Range:', probeResp.headers.get('content-range'));
      console.log('Content-Length:', probeResp.headers.get('content-length'));

      // Test Range >1 MiB
      console.log('\n[10] Testing Range >1 MiB (bytes=0-2097151)...');
      const rangeResp = await fetch(audio[0].url, {
        method: 'GET',
        headers: {
          'User-Agent': 'com.google.android.apps.youtube.vr/1.0.0',
          Range: 'bytes=0-2097151',
        },
        redirect: 'follow',
      });
      console.log('Range >1MiB response status:', rangeResp.status);
      console.log('Content-Range:', rangeResp.headers.get('content-range'));
      console.log('Content-Length:', rangeResp.headers.get('content-length'));

      // Test no Range (full body)
      console.log('\n[11] Testing no-Range (full body)...');
      const fullResp = await fetch(audio[0].url, {
        method: 'GET',
        headers: {
          'User-Agent': 'com.google.android.apps.youtube.vr/1.0.0',
        },
        redirect: 'follow',
      });
      console.log('No-Range response status:', fullResp.status);
      console.log('Content-Length:', fullResp.headers.get('content-length'));
      await fullResp.body?.cancel();
    }

    // Also test MUSIC client with PO token
    console.log('\n=== Testing PO token with MUSIC (WEB_REMIX) client ===');
    const yt3 = await Innertube.create({
      client_type: ClientType.MUSIC,
      generate_session_locally: true,
    });
    const info3 = await yt3.getBasicInfo('rj5wZqReXQE', { po_token: poToken });
    console.log('Playability status:', JSON.stringify(info3.playability_status));
    const sd3 = info3.streaming_data;
    const audio3 = (sd3?.adaptive_formats ?? []).filter((f) => String(f.mime_type ?? '').startsWith('audio/'));
    console.log('Audio formats count:', audio3.length);
    if (audio3[0]?.url) {
      console.log('First audio format:', {
        itag: audio3[0].itag,
        mime_type: audio3[0].mime_type,
        bitrate: audio3[0].average_bitrate,
      });
    }

    console.log('\nDONE - PO token:', poToken);
    return poToken;
  }

  console.log('\nFAILED - could not get integrity token');
  console.log('Full response:', JSON.stringify(waaData));
  return null;
}

generatePoToken().catch(console.error);
