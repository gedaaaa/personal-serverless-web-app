// eslint-disable-next-line @typescript-eslint/triple-slash-reference
/// <reference path="./.sst/platform/config.d.ts" />

export default $config({
  app(input) {
    return {
      name: 'demo-website',
      removal: input?.stage === 'production' ? 'retain' : 'remove',
      protect: ['production'].includes(input?.stage),
      home: 'aws',
    };
  },
  async run() {
    new sst.aws.SvelteKit('DemoWebsite', {
      buildCommand: 'nx run demo-website:build',
      // Intentionall not set domain here, since we are using cloudflare dns and will manually dns cname record in cloudflare
      server: {
        memory: '256 MB',
      },
    });
  },
});
