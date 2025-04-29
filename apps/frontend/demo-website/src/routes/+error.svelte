<script>
  import { page } from '$app/stores';
</script>

<div class="mx-4 flex flex-col items-center justify-center">
  {#if $page.status === 404}
    <h1 class="my-16 text-2xl font-bold text-purple-600">⚠️ Not Found</h1>
    <p class="my-8 text-lg text-gray-500">
      Path `{$page.url.pathname}` does not exist,
      <a class="text-purple-600 underline" href="/">back to home</a>
    </p>
  {:else}
    <h1 class="text-2xl font-bold text-purple-600">
      Unknown Error ({$page.status})
    </h1>
  {/if}
</div>
