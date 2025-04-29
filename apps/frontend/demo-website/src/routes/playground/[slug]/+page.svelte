<script lang="ts">
  import { onMount } from 'svelte';
  import type { Component } from 'svelte';
  import { page } from '$app/state';

  let PostComponent: Component;
  let error: Error | null = null;

  onMount(async () => {
    try {
      const slug = page.params.slug;
      let module;
      try {
        module = await import(`../markdowns/${slug}.svx`);
      } catch {
        module = await import(`../markdowns/${slug}.md`);
      }

      PostComponent = module?.default as Component;
    } catch (e) {
      error = e as Error;
    }
  });
</script>

{#if error && import.meta.env.MODE === 'development'}
  <p>Error: {error.message}</p>
{/if}

{#if PostComponent}
  <PostComponent />
{:else}
  <p>Loding...</p>
{/if}
