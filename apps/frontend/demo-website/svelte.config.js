import adapter from '@sveltejs/adapter-auto';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { mdsvex } from 'mdsvex';

/** @type {import('@sveltejs/kit').Config} */
const config = {
  // Consult https://svelte.dev/docs/kit/integrations
  // for more information about preprocessors
  preprocess: [
    vitePreprocess(),
    mdsvex({
      extensions: ['.md', '.svx'],
      smartypants: {
        dashes: 'oldschool',
      },
      layout: {
        _: 'src/lib/components/markdown/Layout.svelte',
      },
    }),
  ],

  extensions: ['.svelte', '.md', '.svx'],

  kit: {
    // adapter-auto only supports some environments, see https://svelte.dev/docs/kit/adapter-auto for a list.
    // If your environment is not supported, or you settled on a specific environment, switch out the adapter.
    // See https://svelte.dev/docs/kit/adapters for more information about adapters.
    adapter: adapter(),
  },

  // Enable Svelte 5 features
  compilerOptions: {
    // disable force runes mode for compatibility with mdsvex
    // runes: true,
    compatibility: {
      // Gradually migrate to Svelte 5 by enabling both legacy and runes mode
      componentApi: 5,
    },
  },
};

export default config;
