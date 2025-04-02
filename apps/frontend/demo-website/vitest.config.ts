import { defineConfig } from 'vitest/config';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { svelteTesting } from '@testing-library/svelte/vite';
import { resolve } from 'path';

export default defineConfig({
  plugins: [svelte({ hot: false }), svelteTesting()],
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.{test,spec}.{js,ts,svelte.ts}'],
    setupFiles: ['src/setupTests.ts'],
  },
  resolve: {
    alias: {
      $lib: resolve(__dirname, './src/lib'),
      '$app/navigation': resolve(__dirname, './src/mocks/app/navigation.ts'),
      '$app/environment': resolve(__dirname, './src/mocks/app/environment.ts'),
      '$app/state': resolve(__dirname, './src/mocks/app/state.ts'),
    },
  },
});
