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
    host: '0.0.0.0',
    fs: {
      allow: ['../../../node_modules'],
      strict: false, // 放宽文件系统限制
    },
    proxy: {
      '/api': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
    watch: {
      usePolling: true, // 使用轮询模式监听文件变化
      interval: 100, // 轮询间隔
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
