<script>
  import { writable } from 'svelte/store';

  // You can access frontmatter metadata if available
  export let metadata = {};

  // Create a store for the TOC if needed
  const tableOfContents = writable([]);
</script>

<article class="markdown-content">
  <header>
    {#if metadata.title}
      <h1>{metadata.title}</h1>
    {/if}

    {#if metadata.description}
      <p class="description">{metadata.description}</p>
    {/if}

    {#if metadata.date}
      <time datetime={metadata.date}>
        {new Date(metadata.date).toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
        })}
      </time>
    {/if}
  </header>

  <div class="content">
    <slot />
  </div>
</article>

<style>
  .markdown-content {
    max-width: 800px;
    margin: 0 auto;
    padding: 2rem 1rem;
  }

  header {
    margin-bottom: 2rem;
  }

  h1 {
    font-size: 2.5rem;
    font-weight: 700;
    line-height: 1.1;
    margin-bottom: 0.5rem;
  }

  .description {
    font-size: 1.2rem;
    opacity: 0.8;
    margin-bottom: 1rem;
  }

  time {
    font-size: 0.9rem;
    opacity: 0.7;
  }

  .content :global(h2) {
    font-size: 1.8rem;
    font-weight: 600;
    margin: 2rem 0 1rem;
  }

  .content :global(h3) {
    font-size: 1.4rem;
    font-weight: 600;
    margin: 1.5rem 0 0.75rem;
  }

  .content :global(p) {
    margin-bottom: 1.2rem;
    line-height: 1.6;
  }

  .content :global(a) {
    color: #0366d6;
    text-decoration: none;
  }

  .content :global(a:hover) {
    text-decoration: underline;
  }

  .content :global(pre) {
    background: #f6f8fa;
    border-radius: 4px;
    padding: 1rem;
    overflow-x: auto;
    margin: 1rem 0;
  }

  .content :global(code) {
    font-family:
      'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 0.9em;
  }

  .content :global(blockquote) {
    border-left: 4px solid #dfe2e5;
    padding-left: 1rem;
    color: #6a737d;
    margin: 1rem 0;
  }

  .content :global(ul),
  .content :global(ol) {
    padding-left: 2rem;
    margin: 1rem 0;
  }

  .content :global(li) {
    margin-bottom: 0.5rem;
  }

  .content :global(hr) {
    border: none;
    border-top: 1px solid #e1e4e8;
    margin: 2rem 0;
  }

  .content :global(img) {
    max-width: 100%;
    height: auto;
  }
</style>
