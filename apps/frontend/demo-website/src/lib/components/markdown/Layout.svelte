<script>
  import { onMount } from 'svelte';
  // You can access frontmatter metadata if available
  let { date, title, description } = $props();

  onMount(async () => {
    const { default: mermaid } = await import('mermaid');
    mermaid.initialize({ theme: 'dark' });
    await mermaid.run({ querySelector: '.language-mermaid' });
  });
</script>

<article class="mx-auto max-w-3xl px-4 py-8">
  <header class="mb-8">
    {#if title}
      <h1 class="mb-2 text-4xl font-bold leading-tight">{title}</h1>
    {/if}

    {#if description}
      <p class="mb-4 text-xl text-gray-700">{description}</p>
    {/if}

    {#if date}
      <time class="text-sm text-gray-500" datetime={date}>
        {new Date(date).toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
        })}
      </time>
    {/if}
  </header>

  <div class="prose-md prose max-w-none">
    <slot />
  </div>
</article>
