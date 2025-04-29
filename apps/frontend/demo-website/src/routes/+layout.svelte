<script lang="ts">
  import defaultMetadata from '$lib/seo/defaultMetadata';
  import '../app.css';
  import { Header } from '$lib/components/Header';
  let { children } = $props();
</script>

<svelte:head>
  <title>{defaultMetadata.title}</title>
  <meta name="description" content={defaultMetadata.description} />
  <meta name="keywords" content={defaultMetadata.keywords.join(', ')} />

  <meta property="og:title" content={defaultMetadata.title} />
  <meta property="og:description" content={defaultMetadata.description} />
  <meta property="og:image" content={defaultMetadata.ogImage} />
  <meta property="og:type" content="website" />
  <meta property="og:url" content={defaultMetadata.url} />
</svelte:head>

<Header />

<main class="min-w-screen mx-auto min-h-screen bg-gray-50 py-8">
  {@render children()}
</main>
