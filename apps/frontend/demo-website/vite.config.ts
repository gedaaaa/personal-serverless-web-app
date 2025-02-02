/// <reference types='vitest' />
import { defineConfig } from 'vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import { nxCopyAssetsPlugin } from '@nx/vite/plugins/nx-copy-assets.plugin';

const sveltekitFix = async () => {
  const cwd = process.cwd();
  process.chdir(__dirname); // Temporarily change the working directory
  const plugin = await sveltekit(); // Load the SvelteKit plugin
  process.chdir(cwd); // Restore the original working directory
  return plugin;
};

export default defineConfig({
  root: __dirname,
  cacheDir: '../../../node_modules/.vite/demo-website',
  server: {
    port: 4200,
    host: 'localhost',
    fs: {
      allow: ['../../../node_modules'],
    },
  },
  preview: {
    port: 4300,
    host: 'localhost',
  },
  plugins: [sveltekitFix(), nxViteTsPaths(), nxCopyAssetsPlugin(['*.md'])],
  // Uncomment this if you are using workers.
  // worker: {
  //  plugins: [ nxViteTsPaths() ],
  // },
  build: {
    emptyOutDir: true,
    reportCompressedSize: true,
    commonjsOptions: {
      transformMixedEsModules: true,
    },
  },
});
