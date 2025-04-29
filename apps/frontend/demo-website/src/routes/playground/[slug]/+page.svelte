<!-- src/routes/blog/[slug]/+page.svelte -->
<script>
  import { onMount } from 'svelte';
  import { page } from '$app/stores';

  let PostComponent;
  let error = null;

  onMount(async () => {
    try {
      // 动态导入 .svx 文件（客户端加载）
      const slug = $page.params.slug;
      const module = await import(`../markdowns/${slug}.svx`);
      PostComponent = module.default;
    } catch (e) {
      error = e;
    }
  });
</script>

{#if PostComponent}
  <PostComponent />
{:else}
  <p>Loding...</p>
{/if}
