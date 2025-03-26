declare module '*.md' {
  import type { SvelteComponent } from 'svelte';

  export const metadata: Record<string, unknown>;

  const component: SvelteComponent;
  export default component;
}

declare module '*.svx' {
  import type { SvelteComponent } from 'svelte';

  export const metadata: Record<string, unknown>;

  const component: SvelteComponent;
  export default component;
}
