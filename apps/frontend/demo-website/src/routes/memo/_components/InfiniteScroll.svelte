<script lang="ts">
  import { onMount } from 'svelte';
  import memoStore from '../_stores/memoStore.svelte.ts';
  import LoadingSpinner from './LoadingSpinner.svelte';

  // --- State ---
  let sentinelElement: HTMLDivElement;
  let observer: IntersectionObserver;

  // --- Lifecycle ---
  onMount(() => {
    observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        // Use store values directly in the callback
        if (
          entry.isIntersecting &&
          $memoStore.hasMore &&
          !$memoStore.isFetchingList
        ) {
          memoStore.fetchMemos(); // Call store action
        }
      },
      {
        rootMargin: '0px 0px 200px 0px', // Start loading 200px before the element is visible
      },
    );

    // Ensure the sentinel element exists before observing
    const observeSentinel = () => {
      if (sentinelElement) {
        observer.observe(sentinelElement);
      } else {
        // If sentinel is not rendered yet, try again shortly
        requestAnimationFrame(observeSentinel);
      }
    };
    observeSentinel();

    // Cleanup observer on component destroy
    return () => {
      if (observer) {
        observer.disconnect();
      }
    };
  });
</script>

<!-- Infinite loading, status information and load sentinel -->
<div>
  <!-- Sentinel Element for IntersectionObserver -->
  <div bind:this={sentinelElement} class="h-1"></div>

  <!-- Loading indicator for list loads -->
  {#if $memoStore.isFetchingList}
    <LoadingSpinner class="py-4" size="md" />
  {/if}

  <!-- Message when no more items -->
  {#if !$memoStore.hasMore}
    <!-- You might want to add a check here to only show this if there are already items loaded -->
    <p class="py-4 text-center text-gray-500">No more memos found.</p>
  {/if}

  <!-- Consider adding: Message when no items match filter (might need memo count from store) -->
  <!-- {#if !$hasMore && $memos.length === 0 && !$isFetchingList} -->
  <!--   <p class="py-4 text-center text-gray-500"> -->
  <!--     No memos match the current filter. -->
  <!--   </p> -->
  <!-- {/if} -->
</div>
