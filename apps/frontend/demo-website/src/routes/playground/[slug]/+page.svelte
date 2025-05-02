<script lang="ts">
  import type { Component } from 'svelte';
  import defaultMetadata from '$lib/seo/defaultMetadata';

  let {
    data,
  }: {
    data: {
      Content?: Component;
      metadata?: {
        title: string;
        date: string;
        description: string;
        tags: string[];
        ogImage: string;
      };
    };
  } = $props();

  const Content = $derived(data.Content);
  const metadata = $derived(
    data.metadata ?? {
      ...defaultMetadata,
      date: new Date().toISOString(),
      tags: [],
    },
  );

  console.log(data);
  console.log(Content);
</script>

<svelte:head>
  <!-- SEO -->
  <title>{metadata.title}</title>
  <meta name="description" content={metadata.description} />
  <meta name="keywords" content={metadata.tags.join(', ')} />
  <meta name="date" content={metadata.date} />

  <!-- Open Graph -->
  <meta property="og:title" content={metadata.title} />
  <meta property="og:description" content={metadata.description} />
  <meta property="og:image" content={metadata.ogImage} />
  <meta property="og:type" content="article" />

  <!-- Twitter -->
  <meta name="twitter:card" content="summary_large_image" />
  <meta name="twitter:title" content={metadata.title} />
  <meta name="twitter:description" content={metadata.description} />
  <meta name="twitter:image" content={metadata.ogImage} />
</svelte:head>

{#if Content}
  <Content />
{:else}
  <p>Loading...</p>
{/if}
