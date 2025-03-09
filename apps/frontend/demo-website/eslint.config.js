import prettier from 'eslint-config-prettier';
import js from '@eslint/js';
import { includeIgnoreFile } from '@eslint/compat';
import svelte from 'eslint-plugin-svelte';
import globals from 'globals';
import { fileURLToPath } from 'node:url';
import ts from 'typescript-eslint';
const gitignorePath = fileURLToPath(new URL('./.gitignore', import.meta.url));

// Try to import svelte.config.js if it exists
let svelteConfig;
try {
  const svelteConfigPath = fileURLToPath(
    new URL('./svelte.config.js', import.meta.url),
  );
  svelteConfig = await import(svelteConfigPath);
  svelteConfig = svelteConfig.default || svelteConfig;
} catch (error) {
  // Svelte config not found, continue without it
  console.warn(
    'Warning: svelte.config.js not found, some ESLint rules may not work correctly',
  );
}

export default ts.config(
  includeIgnoreFile(gitignorePath),
  js.configs.recommended,
  ...ts.configs.recommended,
  ...svelte.configs['flat/recommended'],
  prettier,
  ...svelte.configs['flat/prettier'],
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
  },
  {
    files: ['**/*.svelte'],
    languageOptions: {
      parserOptions: {
        parser: ts.parser,
        // Add Svelte 5 support
        svelte5: true,
        // Include svelte.config.js if available
        svelteConfig,
      },
    },
    // Add settings for Svelte 5
    settings: {
      svelte: {
        // Ignore TypeScript warnings that might occur in Svelte 5 runes
        ignoreWarnings: [
          '@typescript-eslint/no-unsafe-assignment',
          '@typescript-eslint/no-unsafe-member-access',
          '@typescript-eslint/no-unsafe-call',
        ],
        // Experimental support for Svelte 5
        experimentalFeatures: {
          svelte5: true,
        },
      },
    },
  },
  // 添加测试文件的特殊规则
  {
    files: ['**/*.test.ts', '**/*.test.svelte.ts', '**/__tests__/**/*.ts'],
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-unsafe-assignment': 'off',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      '@typescript-eslint/no-unsafe-return': 'off',
      '@typescript-eslint/no-unsafe-argument': 'off',
    },
  },
  {
    // lint command running from monorepo root, ignores are relative from it.
    ignores: [
      'apps/frontend/demo-website/.svelte-kit/*',
      'apps/frontend/demo-website/.svelte-kit/**/*',
      'apps/frontend/demo-website/sst.config.ts',
    ],
  },
);
