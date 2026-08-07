export type ProviderErrorCode =
  | 'AUTHENTICATION_FAILED'
  | 'RATE_LIMITED'
  | 'GEO_BLOCKED'
  | 'NOT_FOUND'
  | 'UNSUPPORTED'
  | 'PLAYBACK_ERROR'
  | 'NETWORK_ERROR'
  | 'TIMEOUT_ERROR'
  | 'INTERNAL_ERROR';

export class ProviderError extends Error {
  public readonly code: ProviderErrorCode;
  public readonly providerId: string;
  public readonly statusCode: number;
  public readonly retryAfterSeconds?: number;

  constructor(
    code: ProviderErrorCode,
    message: string,
    providerId: string,
    statusCode: number = 500,
    retryAfterSeconds?: number
  ) {
    super(message);
    this.name = 'ProviderError';
    this.code = code;
    this.providerId = providerId;
    this.statusCode = statusCode;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export class AuthenticationFailedError extends ProviderError {
  constructor(message: string, providerId: string) {
    super('AUTHENTICATION_FAILED', message, providerId, 401);
  }
}

export class RateLimitedError extends ProviderError {
  constructor(message: string, providerId: string, retryAfterSeconds: number = 60) {
    super('RATE_LIMITED', message, providerId, 429, retryAfterSeconds);
  }
}

export class GeoBlockedError extends ProviderError {
  constructor(message: string, providerId: string) {
    super('GEO_BLOCKED', message, providerId, 403);
  }
}

export class NotFoundError extends ProviderError {
  constructor(message: string, providerId: string) {
    super('NOT_FOUND', message, providerId, 404);
  }
}

export class UnsupportedError extends ProviderError {
  constructor(message: string, providerId: string) {
    super('UNSUPPORTED', message, providerId, 501);
  }
}

export class PlaybackError extends ProviderError {
  constructor(message: string, providerId: string) {
    super('PLAYBACK_ERROR', message, providerId, 502);
  }
}

export class NetworkError extends ProviderError {
  constructor(message: string, providerId: string) {
    super('NETWORK_ERROR', message, providerId, 503);
  }
}

export class TimeoutError extends ProviderError {
  constructor(message: string, providerId: string) {
    super('TIMEOUT_ERROR', message, providerId, 504);
  }
}

export class InternalError extends ProviderError {
  constructor(message: string, providerId: string) {
    super('INTERNAL_ERROR', message, providerId, 500);
  }
}
