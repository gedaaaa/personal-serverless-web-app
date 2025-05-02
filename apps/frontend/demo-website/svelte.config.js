import adapter from '@sveltejs/adapter-auto';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { mdsvex, escapeSvelte } from 'mdsvex';
import { createHighlighter } from 'shiki';
import rehypeMermaid from 'rehype-mermaid';

let highlighter;

export async function highlight(code, lang) {
  if (!highlighter) {
    highlighter = await createHighlighter({
      themes: ['github-dark'],
      langs: ['ts', 'svelte'],
    });
  }
  return escapeSvelte(
    await highlighter.codeToHtml(code, { lang, theme: 'github-dark' }),
  );
}
/** @type {import('@sveltejs/kit').Config} */
const config = {
  // Consult https://svelte.dev/docs/kit/integrations
  // for more information about preprocessors
  preprocess: [
    vitePreprocess(),
    mdsvex({
      rehypePlugins: [[rehypeMermaid, { strategy: 'img-svg' }]],
      highlight: {
        // return type of mdsvex default highlighter and shiki highlighter is string,
        // which will be transformed to 'raw' block with returned content(html string).
        // but rehype-mermaid can not handle this, it requires an element node with type 'code' and className 'language-mermaid' in AST
        // so we manually transform the code block to the required AST node here
        highlighter: (code, lang) => {
          if (lang === 'mermaid') {
            return {
              type: 'element',
              tagName: 'code',
              properties: { className: 'language-mermaid' },
              children: [{ type: 'text', value: code }],
            };
          }
          return highlight(code, lang);
        },
      },
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
      componentApi: 4,
    },
  },
};

export default config;
