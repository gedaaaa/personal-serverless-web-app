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
      domain: {
        name: 'sunbath.top',
        dns: sst.cloudflare.dns({
          zone: '678cdfc9ae2d55f9a3097c96528f5754',
        }),
      },
      redirects: ['www.sunbath.top'],
      server: {
        memory: '256 MB',
      },
    });
  },
});
