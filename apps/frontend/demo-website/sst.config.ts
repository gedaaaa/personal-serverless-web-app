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
        // exclude @resvg/resvg-js from svelte-kit build,
        // and install it in node_modules.
        // Because this library has .node file to run.
        install: ['@resvg/resvg-js'],
        memory: '256 MB',
      },
    });
  },
});
