import fs from 'fs';
import path from 'path';
import YAML from 'yaml';

export interface GatewayConfig {
  version: string;
  server: {
    port: number;
    host: string;
    corsOrigins: string[];
    gatewayVersion: string;
    apiVersion: string;
    minimumAndroidVersion: string;
  };
  providers: Record<
    string,
    {
      enabled: boolean;
      priority: number;
      circuitBreaker?: {
        failureThreshold: number;
        cooldownSeconds: number;
      };
      instanceUrl?: string;
    }
  >;
  cache: {
    redisUrl: string;
    metadataTTLSeconds: number;
    searchTTLSeconds: number;
    streamTTLSeconds: number;
    artworkTTLSeconds: number;
  };
  stream: {
    validateHeadRequests: boolean;
    urlRefreshBufferSeconds: number;
  };
}

export function loadConfig(configPath?: string): GatewayConfig {
  const defaultPath = path.resolve(process.cwd(), 'config/gateway.yaml');
  const targetPath = configPath || process.env.GATEWAY_CONFIG_PATH || defaultPath;

  let fileContent = '';
  if (fs.existsSync(targetPath)) {
    fileContent = fs.readFileSync(targetPath, 'utf8');
  } else {
    // Fallback default config if file missing
    return {
      version: '1.0',
      server: {
        port: parseInt(process.env.PORT || '8080', 10),
        host: process.env.HOST || '0.0.0.0',
        corsOrigins: ['*'],
        gatewayVersion: '1.0.0',
        apiVersion: '1.0.0',
        minimumAndroidVersion: '1.0.0',
      },
      providers: {
        mock: { enabled: true, priority: 100 },
      },
      cache: {
        redisUrl: process.env.REDIS_URL || 'redis://localhost:6379',
        metadataTTLSeconds: 86400,
        searchTTLSeconds: 3600,
        streamTTLSeconds: 900,
        artworkTTLSeconds: 604800,
      },
      stream: {
        validateHeadRequests: true,
        urlRefreshBufferSeconds: 300,
      },
    };
  }

  const parsed = YAML.parse(fileContent) as GatewayConfig;
  if (process.env.PORT) parsed.server.port = parseInt(process.env.PORT, 10);
  if (process.env.REDIS_URL) parsed.cache.redisUrl = process.env.REDIS_URL;
  return parsed;
}
