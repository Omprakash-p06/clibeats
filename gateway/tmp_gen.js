const https = require('https');
const { JSDOM, VirtualConsole } = require('youtube-po-token-generator/node_modules/jsdom');
const fs = require('fs');
const path = require('path');

// Fetch visitor data from YouTube embed
const embedUrl = 'https://www.youtube.com/embed/dQw4w9WgXcQ';
const options = {
  hostname: 'www.youtube.com',
  path: '/embed/dQw4w9WgXcQ',
  method: 'GET',
  headers: {
    'user-agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)',
    'accept-language': 'en-US,en;q=0.9',
  }
};

const req = https.request(options, (res) => {
  let data = '';
  res.on('data', (chunk) => data += chunk);
  res.on('end', () => {
    const matched = data.match(/"visitorData":"([^"]+)"/);
    if (!matched) {
      console.error('Failed to find visitorData');
      return;
    }
    const visitorData = matched[1];
    console.log('Visitor data:', visitorData.substring(0, 50) + '...');

    // Read vendor files
    const domContent = fs.readFileSync(path.join('node_modules/youtube-po-token-generator/vendor', 'index.html'), 'utf-8');
    const baseContent = fs.readFileSync(path.join('node_modules/youtube-po-token-generator/vendor', 'base.js'), 'utf-8');
    const injectContent = fs.readFileSync(path.join('node_modules/youtube-po-token-generator/lib', 'inject.js'), 'utf-8');

    // Use original index.html which has ytcfg
    const dom = new JSDOM(domContent, {
      url: embedUrl,
      pretendToBeVisual: true,
      runScripts: 'dangerously',
      virtualConsole: new VirtualConsole(),
    });
    const window = dom.window;

    Object.defineProperty(window.navigator, 'userAgent', {
      value: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)',
      writable: false
    });
    window.visitorData = visitorData;

    let poToken = null;
    let resolved = false;

    window.onPoToken = (token) => {
      if (resolved) return;
      resolved = true;
      poToken = token;
      console.log('Got PO token! Length:', token.length);
      console.log('Full token:', token);
      window.close();
      process.exit(0);
    };

    console.log('Evaluating player JS...');
    try {
      // Inject the inject.js code before the player initialization
      const modifiedBase = baseContent.replace(/\}\s*\)\(_yt_player\);\s*$/, (matched) => `;${injectContent};${matched}`);
      window.eval(modifiedBase);
    } catch (e) {
      console.error('Error evaluating player JS:', e.message);
    }

    // Timeout after 60s
    setTimeout(() => {
      if (!resolved) {
        console.error('No PO token received after 60s timeout');
        window.close();
        process.exit(1);
      }
    }, 60000);
  });
});

req.on('error', (e) => {
  console.error('Error fetching embed page:', e.message);
});
req.end();