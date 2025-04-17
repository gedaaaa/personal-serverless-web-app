<script lang="ts">
  import type { Snippet } from 'svelte';

  // type Props = {
  // 	onAddClick: () => void;
  //  children: Snippet;
  // };
  // const { onAddClick, children }: Props = $props();
  let { onAddClick }: { onAddClick: () => void } = $props();
</script>

<button
  onclick={onAddClick}
  class="flex items-center rounded-full bg-purple-600 p-3 text-white shadow-sm hover:bg-purple-700 focus:outline-none"
  aria-label="Add new memo"
>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    class="h-6 w-6"
    fill="none"
    viewBox="0 0 24 24"
    stroke="currentColor"
  >
    <path
      stroke-linecap="round"
      stroke-linejoin="round"
      stroke-width="2"
      d="M12 6v6m0 0v6m0-6h6m-6 0H6"
    />
  </svg>
</button>
