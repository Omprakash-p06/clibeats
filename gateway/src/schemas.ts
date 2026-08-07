export type JsonSchema = Record<string, unknown>;

export const TrackSchema: JsonSchema = {
  type: 'object',
  properties: {
    id: { type: 'string', description: 'Provider-scoped track identifier' },
    providerId: { type: 'string', description: 'Adapter that resolved this track' },
    title: { type: 'string' },
    artist: { type: 'string' },
    album: { type: 'string' },
    durationSeconds: { type: 'number' },
    artworkUrl: { type: 'string', format: 'uri' },
    explicit: { type: 'boolean' },
  },
  required: ['id', 'providerId', 'title', 'artist', 'durationSeconds'],
  additionalProperties: true,
};

export const AlbumSchema: JsonSchema = {
  type: 'object',
  properties: {
    id: { type: 'string' },
    providerId: { type: 'string' },
    title: { type: 'string' },
    artist: { type: 'string' },
    artworkUrl: { type: 'string', format: 'uri' },
    trackCount: { type: 'number' },
    releaseYear: { type: 'number' },
    tracks: { type: 'array', items: TrackSchema },
  },
  required: ['id', 'providerId', 'title', 'artist', 'trackCount', 'tracks'],
  additionalProperties: true,
};

export const ArtistSchema: JsonSchema = {
  type: 'object',
  properties: {
    id: { type: 'string' },
    providerId: { type: 'string' },
    name: { type: 'string' },
    avatarUrl: { type: 'string', format: 'uri' },
    bio: { type: 'string' },
  },
  required: ['id', 'providerId', 'name'],
  additionalProperties: true,
};

export const PlaylistSchema: JsonSchema = {
  type: 'object',
  properties: {
    id: { type: 'string' },
    providerId: { type: 'string' },
    title: { type: 'string' },
    description: { type: 'string' },
    artworkUrl: { type: 'string', format: 'uri' },
    trackCount: { type: 'number' },
    tracks: { type: 'array', items: TrackSchema },
  },
  required: ['id', 'providerId', 'title', 'trackCount', 'tracks'],
  additionalProperties: true,
};

export const StreamResultSchema: JsonSchema = {
  type: 'object',
  properties: {
    trackId: { type: 'string' },
    providerId: { type: 'string' },
    streamUrl: { type: 'string', format: 'uri' },
    mimeType: { type: 'string' },
    bitrateKbps: { type: 'number' },
    expiresAtEpochSeconds: { type: 'number' },
    headers: { type: 'object', additionalProperties: { type: 'string' } },
  },
  required: ['trackId', 'providerId', 'streamUrl', 'mimeType', 'expiresAtEpochSeconds'],
  additionalProperties: true,
};

export const AdapterHealthSchema: JsonSchema = {
  type: 'object',
  properties: {
    status: { type: 'string', enum: ['HEALTHY', 'DEGRADED', 'UNHEALTHY'] },
    score: { type: 'number', minimum: 0, maximum: 100 },
    latencyMs: { type: 'number' },
    message: { type: 'string' },
  },
  required: ['status', 'score', 'latencyMs'],
};

export const ProviderCapabilitiesSchema: JsonSchema = {
  type: 'object',
  properties: {
    search: { type: 'boolean' },
    playback: { type: 'boolean' },
    playlists: { type: 'boolean' },
    albums: { type: 'boolean' },
    artists: { type: 'boolean' },
    recommendations: { type: 'boolean' },
    radio: { type: 'boolean' },
    downloads: { type: 'boolean' },
    lyrics: { type: 'boolean' },
  },
  additionalProperties: true,
};

export const ProviderInfoSchema: JsonSchema = {
  type: 'object',
  properties: {
    id: { type: 'string' },
    name: { type: 'string' },
    status: { type: 'string', enum: ['HEALTHY', 'DEGRADED', 'UNHEALTHY'] },
    score: { type: 'number' },
    latencyMs: { type: 'number' },
    priority: { type: 'number' },
    capabilities: ProviderCapabilitiesSchema,
  },
  required: ['id', 'name'],
};

export const ErrorResponseSchema: JsonSchema = {
  type: 'object',
  properties: {
    error: {
      type: 'object',
      properties: {
        code: {
          type: 'string',
          enum: [
            'AUTHENTICATION_FAILED',
            'RATE_LIMITED',
            'GEO_BLOCKED',
            'NOT_FOUND',
            'UNSUPPORTED',
            'PLAYBACK_ERROR',
            'NETWORK_ERROR',
            'TIMEOUT_ERROR',
            'INTERNAL_ERROR',
            'INVALID_REQUEST',
          ],
        },
        message: { type: 'string' },
        providerId: { type: 'string' },
        retryAfterSeconds: { type: 'number' },
        traceId: { type: 'string' },
      },
      required: ['code', 'message', 'providerId', 'traceId'],
    },
  },
  required: ['error'],
};

const idParamSchema: JsonSchema = {
  type: 'object',
  properties: { id: { type: 'string' } },
  required: ['id'],
};

export const bootstrapSchema = {
  tags: ['Bootstrap'],
  description: 'Aggregated cold-start initialization context: version, capabilities, provider health.',
  summary: 'Fetch gateway bootstrap context',
  response: {
    200: {
      type: 'object',
      properties: {
        gatewayVersion: { type: 'string' },
        apiVersion: { type: 'string' },
        minimumAndroidVersion: { type: 'string' },
        supportedProviders: { type: 'array', items: ProviderInfoSchema },
        features: {
          type: 'object',
          properties: {
            directToCdnStreaming: { type: 'boolean' },
            streamUrlAutoRefresh: { type: 'boolean' },
            circuitBreakerEnabled: { type: 'boolean' },
          },
        },
        configuration: {
          type: 'object',
          properties: { streamRefreshBufferSeconds: { type: 'number' } },
        },
      },
      required: ['gatewayVersion', 'apiVersion', 'minimumAndroidVersion', 'supportedProviders'],
    },
    500: ErrorResponseSchema,
  },
};

export const searchSchema = {
  tags: ['Search'],
  description: 'Search tracks across all available providers with automatic failover.',
  summary: 'Search for tracks',
  querystring: {
    type: 'object',
    properties: {
      q: { type: 'string', description: 'Search query (case-insensitive)', examples: ['cyberpunk'] },
      filterSongs: {
        type: 'boolean',
        default: true,
        description: 'Return only songs (excludes podcasts/shows)',
      },
    },
  },
  response: {
    200: {
      type: 'object',
      properties: {
        tracks: { type: 'array', items: TrackSchema },
        cached: { type: 'boolean' },
      },
      required: ['tracks', 'cached'],
    },
    400: ErrorResponseSchema,
    500: ErrorResponseSchema,
    503: ErrorResponseSchema,
  },
};

export const albumSchema = {
  tags: ['Albums'],
  description: 'Fetch album details including full tracklist.',
  summary: 'Get album by id',
  params: idParamSchema,
  response: {
    200: {
      type: 'object',
      properties: { album: AlbumSchema, cached: { type: 'boolean' } },
      required: ['album', 'cached'],
    },
    404: ErrorResponseSchema,
    500: ErrorResponseSchema,
    503: ErrorResponseSchema,
  },
};

export const artistSchema = {
  tags: ['Artists'],
  description: 'Fetch artist profile.',
  summary: 'Get artist by id',
  params: idParamSchema,
  response: {
    200: {
      type: 'object',
      properties: { artist: ArtistSchema, cached: { type: 'boolean' } },
      required: ['artist', 'cached'],
    },
    404: ErrorResponseSchema,
    500: ErrorResponseSchema,
    503: ErrorResponseSchema,
  },
};

export const playlistSchema = {
  tags: ['Playlists'],
  description: 'Fetch playlist details and its tracks.',
  summary: 'Get playlist by id',
  params: idParamSchema,
  response: {
    200: {
      type: 'object',
      properties: { playlist: PlaylistSchema, cached: { type: 'boolean' } },
      required: ['playlist', 'cached'],
    },
    404: ErrorResponseSchema,
    500: ErrorResponseSchema,
    503: ErrorResponseSchema,
  },
};

export const streamSchema = {
  tags: ['Playback'],
  description: 'Resolve a direct-to-CDN audio stream URL for a track.',
  summary: 'Resolve stream URL',
  body: {
    type: 'object',
    properties: { trackId: { type: 'string' } },
    required: ['trackId'],
    examples: [{ trackId: 'mock-track-1' }],
  },
  response: {
    200: {
      type: 'object',
      properties: { stream: StreamResultSchema },
      required: ['stream'],
    },
    400: ErrorResponseSchema,
    404: ErrorResponseSchema,
    502: ErrorResponseSchema,
    503: ErrorResponseSchema,
  },
};

export const healthSchema = {
  tags: ['Observability'],
  description: 'Machine-readable aggregate health of gateway, Redis, and providers.',
  summary: 'Gateway health',
  response: {
    200: {
      type: 'object',
      properties: {
        gateway: { type: 'string', enum: ['HEALTHY', 'DEGRADED', 'UNHEALTHY'] },
        redis: { type: 'string', enum: ['UP', 'DEGRADED', 'DOWN'] },
        redisLatencyMs: { type: 'number' },
        providers: {
          type: 'object',
          additionalProperties: AdapterHealthSchema,
        },
        uptime: { type: 'number' },
        version: { type: 'string' },
      },
      required: ['gateway', 'redis', 'providers', 'uptime', 'version'],
    },
    500: ErrorResponseSchema,
  },
};

export const versionSchema = {
  tags: ['Observability'],
  description: 'Gateway version information.',
  summary: 'Gateway version',
  response: {
    200: {
      type: 'object',
      properties: { version: { type: 'string' } },
      required: ['version'],
    },
  },
};

export const metricsSchema = {
  tags: ['Observability'],
  description: 'Prometheus metrics exporter. Returns text/plain exposition format.',
  summary: 'Prometheus metrics',
  response: {
    200: {
      type: 'string',
      description: 'Prometheus text exposition format',
    },
  },
};

export const providersSchema = {
  tags: ['Providers'],
  description: 'List all registered provider adapters with priority and capabilities.',
  summary: 'List providers',
  response: {
    200: {
      type: 'object',
      properties: { providers: { type: 'array', items: ProviderInfoSchema } },
      required: ['providers'],
    },
  },
};
