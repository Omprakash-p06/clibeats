import { Innertube } from 'youtubei.js';
import fs from 'node:fs';
const yt = await Innertube.create({ client_type: 'WEB', generate_session_locally: true });
const c = await yt.getAttestationChallenge('ENGAGEMENT_TYPE_UNBOUND');
const url = 'https:' + c.bg_challenge.interpreter_url.private_do_not_access_or_else_trusted_resource_url_wrapped_value;
const js = await (await fetch(url)).text();
fs.writeFileSync('interpreter.js', js);
console.log('saved', js.length, url);
