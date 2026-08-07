export interface ProviderContext {
  country: string;
  language: string;
  authenticated: boolean;
  preferredAudioQuality: 'LOW' | 'MEDIUM' | 'HIGH' | 'LOSSLESS';
  device: 'mobile' | 'desktop';
  traceId: string;
}
