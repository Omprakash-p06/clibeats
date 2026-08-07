declare module 'ioredis-mock' {
  import Redis from 'ioredis';
  const RedisMock: new (...args: any[]) => Redis;
  export default RedisMock;
}

declare module 'autocannon';
