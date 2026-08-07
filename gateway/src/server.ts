import { buildApp } from './app';
import { logger } from './core/logging/logger';

const app = buildApp();
const port = app.config.server.port;
const host = app.config.server.host;

const start = async () => {
  try {
    await app.listen({ port, host });
    logger.info(`CliBeats Provider Gateway running on http://${host}:${port}`);
    logger.info(`OpenAPI Swagger documentation available on http://${host}:${port}/documentation`);
  } catch (err) {
    logger.error(err, 'Failed to start gateway server');
    process.exit(1);
  }
};

const shutdown = async (signal: string) => {
  logger.info(`Received ${signal}. Shutting down gateway gracefully...`);
  try {
    await app.close();
    logger.info('Gateway server stopped cleanly.');
    process.exit(0);
  } catch (err) {
    logger.error(err, 'Error during shutdown');
    process.exit(1);
  }
};

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));

start();
