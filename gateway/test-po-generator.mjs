// test-po-generator.mjs
import { Innertube, ClientType } from 'youtubei.js';

async function main() {
  console.log('[1] Generating PO token with youtube-po-token-generator...');
  const mod = await import('youtube-po-token-generator');
  const { visitorData, poToken } = await mod.generate();
  console.log('Visitor data:', visitorData);
  console.log('PO token length:', poToken.length);
  console.log('PO token:', poToken);
  
  // Test PO token with each client type
  const clients = [
    { name: 'MUSIC', type: ClientType.MUSIC },
    { name: 'ANDROID_VR', type: ClientType.ANDROID_VR },
    { name: 'ANDROID', type: ClientType.ANDROID },
    { name: 'WEB', type: ClientType.WEB },
    { name: 'TV', type: ClientType.TV },
    { name: 'TV_EMBEDDED', type: ClientType.TV_EMBEDDED },
  ];

  for (const { name, type } of clients) {
    console.log(`\n=== Testing ${name} (${type}) with PO token ===`);
    try {
      const yt = await Innertube.create({
        client_type: type,
        generate_session_locally: true,
        po_token: poToken,
      });
      const info = await yt.getBasicInfo('rj5wZqReXQE', { po_token: poToken });
      
      const sd = info.streaming_data;
      const audio = (sd?.adaptive_formats ?? []).filter((f) =>
        String(f.mime_type ?? '').startsWith('audio/')
      );
      
      console.log('  Playability status:', JSON.stringify(info.playability_status));
      console.log('  streamingData present:', sd ? 'YES' : 'NO');
      console.log('  formatsCount:', sd?.adaptive_formats?.length ?? 0);
      console.log('  audioFormatsCount:', audio.length);
      
      if (audio.length > 0) {
        const fmt = audio[0];
        console.log('  Selected format:', {
          itag: fmt.itag,
          mime_type: fmt.mime_type,
          bitrate: fmt.average_bitrate,
          hasUrl: !!fmt.url,
        });
        
        if (fmt.url) {
          // Test HTTP status with HEAD request
          console.log('  Testing URL HTTP status (HEAD, Range bytes=0-0)...');
          const headResp = await fetch(fmt.url, {
            method: 'GET',
            headers: {
              'Range': 'bytes=0-0',
              'User-Agent': 'com.google.android.apps.youtube.vr/1.0.0 (Android 13)',
            },
            redirect: 'follow',
          });
          console.log('  HTTP status:', headResp.status);
          console.log('  Content-Range:', headResp.headers.get('content-range'));
          console.log('  Content-Length:', headResp.headers.get('content-length'));
          await headResp.body?.cancel();
          
          // Test Range >1 MiB
          console.log('  Testing Range >1 MiB (bytes=0-2097151)...');
          const rangeResp = await fetch(fmt.url, {
            method: 'GET',
            headers: {
              'Range': 'bytes=0-2097151',
              'User-Agent': 'com.google.android.apps.youtube.vr/1.0.0 (Android 13)',
            },
            redirect: 'follow',
          });
          console.log('  Range >1MiB HTTP status:', rangeResp.status);
          console.log('  Content-Range:', rangeResp.headers.get('content-range'));
          console.log('  Content-Length:', rangeResp.headers.get('content-length'));
          await rangeResp.body?.cancel();
          
          // Test no-Range (full body)
          console.log('  Testing no-Range (full body, first byte)...');
          const fullResp = await fetch(fmt.url, {
            method: 'GET',
            headers: {
              'Range': 'bytes=0-0',
              'User-Agent': 'com.google.android.apps.youtube.vr/1.0.0 (Android 13)',
            },
            redirect: 'follow',
          });
          console.log('  No-Range HTTP status:', fullResp.status);
          const cl = fullResp.headers.get('content-length');
          console.log('  Content-Length:', cl);
          console.log('  Long-duration (>1MiB body):', cl && parseInt(cl) > 1048576 ? 'YES' : 'NO');
          await fullResp.body?.cancel();
        }
      }
    } catch (err) {
      console.log('  ERROR:', err.message);
    }
  }
}

main().catch(console.error);
