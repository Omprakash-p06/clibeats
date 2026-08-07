import { ProviderAdapter } from '../../types/adapter';

export class ProviderRegistry {
  private adapters: Map<string, ProviderAdapter> = new Map();

  public register(adapter: ProviderAdapter): void {
    this.adapters.set(adapter.id, adapter);
  }

  public get(id: string): ProviderAdapter | undefined {
    return this.adapters.get(id);
  }

  public getAll(): ProviderAdapter[] {
    return Array.from(this.adapters.values());
  }

  public getSortedByPriority(): ProviderAdapter[] {
    return this.getAll().sort((a, b) => b.priority - a.priority);
  }

  public clear(): void {
    this.adapters.clear();
  }
}
