import { globalEventBus } from '../events/EventBus';
import { recordCircuitBreakerState } from '../metrics/metrics';

export type CircuitState = 'CLOSED' | 'OPEN' | 'HALF_OPEN';

export class CircuitBreaker {
  private state: CircuitState = 'CLOSED';
  private failureCount: number = 0;
  private lastStateChangeEpoch: number = Date.now();

  constructor(
    public readonly providerId: string,
    private failureThreshold: number = 3,
    private cooldownSeconds: number = 60
  ) {
    recordCircuitBreakerState(this.providerId, this.state);
  }

  public getState(): CircuitState {
    if (this.state === 'OPEN') {
      const elapsed = (Date.now() - this.lastStateChangeEpoch) / 1000;
      if (elapsed >= this.cooldownSeconds) {
        this.state = 'HALF_OPEN';
        this.lastStateChangeEpoch = Date.now();
        recordCircuitBreakerState(this.providerId, this.state);
      }
    }
    return this.state;
  }

  public recordSuccess(): void {
    this.failureCount = 0;
    if (this.state !== 'CLOSED') {
      this.state = 'CLOSED';
      this.lastStateChangeEpoch = Date.now();
      recordCircuitBreakerState(this.providerId, this.state);
    }
  }

  public recordFailure(): void {
    this.failureCount++;
    if (this.failureCount >= this.failureThreshold && this.state !== 'OPEN') {
      this.state = 'OPEN';
      this.lastStateChangeEpoch = Date.now();
      recordCircuitBreakerState(this.providerId, this.state);
      globalEventBus.emitEvent({
        type: 'CIRCUIT_TRIPPED',
        providerId: this.providerId,
        failureCount: this.failureCount,
      });
    }
  }

  public isAvailable(): boolean {
    return this.getState() !== 'OPEN';
  }
}
