export const YOUTUBE_PROVIDER_ID = 'youtube';

const ARTWORK_RE = /(?!s\d+-)w\d+-h\d+/;

export function largestArtworkUrl(urls: Array<string | undefined> | undefined): string | undefined {
  if (!urls) return undefined;
  const valid = urls.filter((u): u is string => typeof u === 'string' && u.length > 0);
  if (valid.length === 0) return undefined;
  if (valid.length === 1) return valid[0];
  return valid.find((u) => ARTWORK_RE.test(u)) ?? valid[valid.length - 1];
}

export interface MusicItemSnapshot {
  id?: string;
  itemType?: string;
  title?: string;
  /** Raw column text, e.g. ['Song Title', 'Song • Artist', '4.5M plays']. */
  columns?: string[];
  artists?: Array<{ name?: string; id?: string }>;
  albumName?: string;
  durationSeconds?: number;
  artworkUrl?: string;
}

export function parseRawItem(item: unknown): MusicItemSnapshot {
  if (!item || typeof item !== 'object') return {};
  const it = item as Record<string, unknown>;

  let id: string | undefined = typeof it.id === 'string' ? it.id : undefined;
  let itemType: string | undefined = typeof it.item_type === 'string' ? it.item_type : undefined;

  // Extract title
  let title: string | undefined = undefined;
  if (typeof it.title === 'string') {
    title = it.title;
  } else if (it.title && typeof it.title === 'object' && typeof (it.title as any).text === 'string') {
    title = (it.title as any).text;
  } else if (typeof it.name === 'string') {
    title = it.name;
  }

  const flex = Array.isArray(it.flex_columns) ? it.flex_columns : [];
  const columns = flex.map((f) => {
    const col = f as { title?: { text?: string } };
    return typeof col?.title?.text === 'string' ? col.title.text : undefined;
  }).filter((t): t is string => t !== undefined);

  if (!title && columns.length > 0) {
    title = columns[0];
  }

  // Channel/Artist items start with UC
  if (id && id.startsWith('UC')) {
    itemType = 'artist';
  } else if (id && id.length === 11) {
    itemType = 'song';
  }

  const snapshot: MusicItemSnapshot = {
    id,
    itemType,
    title,
    columns,
  };

  const artists = it.artists;
  if (Array.isArray(artists)) {
    snapshot.artists = artists.map((a) => {
      const ar = a as { name?: string; channelId?: string; id?: string };
      return { name: ar?.name, id: ar?.channelId ?? ar?.id };
    });
  } else {
    snapshot.artists = [];
  }

  const album = it.album as { name?: string; id?: string } | undefined;
  if (album && typeof album === 'object') {
    snapshot.albumName = album.name;
  }

  const duration = it.duration as { seconds?: number } | undefined;
  if (duration && typeof duration === 'object' && typeof duration.seconds === 'number') {
    snapshot.durationSeconds = duration.seconds;
  }

  const thumb = it.thumbnail as
    | { contents?: Array<{ url?: string }> }
    | { urls?: Array<{ url?: string }> }
    | undefined;
  if (thumb && typeof thumb === 'object') {
    const contentsArr = (thumb as { contents?: Array<{ url?: string }> }).contents;
    const urlsArr = (thumb as { urls?: Array<{ url?: string }> }).urls;
    const contents = Array.isArray(contentsArr)
      ? contentsArr.map((t) => t.url)
      : Array.isArray(urlsArr)
        ? urlsArr.map((t) => t.url)
        : undefined;
    snapshot.artworkUrl = largestArtworkUrl(contents);
  }

  return snapshot;
}

/** Parse artist/album out of a formatted subtitle like 'Song • Artist' / 'Album • 2013'. */
export function parseSubtitle(subtitle?: string): { artist?: string; album?: string } {
  if (!subtitle) return {};
  const normalized = subtitle.replace(/^track|^song|^album|^single\s*•\s*/i, '').trim();
  if (!normalized) return {};
  const parts = normalized.split('•').map((p) => p.trim()).filter(Boolean);
  if (parts.length >= 2) return { artist: parts[0], album: parts[1] };
  return { artist: parts[0] };
}