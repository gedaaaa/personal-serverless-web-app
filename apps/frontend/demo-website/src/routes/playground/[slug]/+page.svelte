<script lang="ts">
  import type { Component } from 'svelte';

  export let data: {
    Content?: Component;
    metadata: {
      title: string;
      date: string;
      description: string;
      tags: string[];
    };
  };
  const Content = data.Content;
</script>

{#if Content}
  <Content />
{:else}
  <p>Loading...</p>
{/if}
