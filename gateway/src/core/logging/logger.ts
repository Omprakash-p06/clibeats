import pino from 'pino';
import { globalEventBus, GatewayEventPayload } from '../events/EventBus';

export const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  formatters: {
    level: (label) => ({ level: label }),
  },
  base: { service: 'clibeats-gateway' },
  timestamp: pino.stdTimeFunctions.isoTime,
});

// Attach event bus listener for automated structured logging
globalEventBus.onEvent('*', (event: GatewayEventPayload) => {
  logger.info({ eventType: event.type, ...event }, `Event: ${event.type}`);
});
