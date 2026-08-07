import { EventEmitter } from 'events';

export type GatewayEventPayload =
  | { type: 'REQUEST_RECEIVED'; traceId: string; endpoint: string; clientIp?: string }
  | { type: 'CACHE_CHECKED'; traceId: string; namespace: string; hit: boolean }
  | { type: 'PROVIDER_SELECTED'; traceId: string; providerId: string; score: number }
  | { type: 'PROVIDER_FAILED'; traceId: string; providerId: string; error: string }
  | { type: 'PROVIDER_FAILOVER'; traceId: string; fromProvider: string; toProvider: string }
  | { type: 'STREAM_RESOLVED'; traceId: string; trackId: string; durationMs: number }
  | { type: 'CIRCUIT_TRIPPED'; providerId: string; failureCount: number };

export class EventBus extends EventEmitter {
  public emitEvent(event: GatewayEventPayload): void {
    this.emit(event.type, event);
    this.emit('*', event);
  }

  public onEvent(type: string, listener: (event: GatewayEventPayload) => void): void {
    this.on(type, listener);
  }
}

export const globalEventBus = new EventBus();
