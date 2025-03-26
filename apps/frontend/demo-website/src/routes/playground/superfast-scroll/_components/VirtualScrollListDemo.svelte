<script lang="ts">
  import { onMount } from 'svelte';
  import VirtualScrollList from './internal/components/VirtualScrollList.svelte';
  import type { DataItem, DataSource } from './data/DataSource/DataSource';
  import { DelayedDataSource } from './data/DataSource/DelayedDataSource';

  /**
   * Defines the structure of items to be displayed in the virtual scroll list.
   */
  interface DemoItem extends DataItem {
    id: number;
    name: string;
    description: string;
  }

  /**
   * Application state for the virtual scroll demo.
   */
  let dataSource: DataSource<DemoItem> | null = $state(null);
  let jumpToPosition: (position: number) => void = $state(() => {});
  let jumpTarget = $state(0);
  let totalItems = $state(0);
  let isInitializing = $state(true);

  const visibleItemsCount = 10;

  /**
   * Initializes the data source with a large set of demo items.
   * Creates a million items and makes them available for the virtual scroll list.
   */
  onMount(() => {
    isInitializing = true;
    const initialDataSource = new DelayedDataSource<DemoItem>();

    // Generate demo items
    for (let i = 0; i < 1000000; i++) {
      const item: DemoItem = {
        id: i,
        name: `Item ${i}`,
        description: `This is the description for item ${i}`,
      };
      initialDataSource.insert(item);
    }

    dataSource = initialDataSource;

    // Update total items count
    Promise.resolve(dataSource.getTotalCount()).then((count) => {
      totalItems = count;
    });
    jumpToPosition(0);
    isInitializing = false;
  });

  /**
   * Navigates to the specified position in the list when triggered.
   */
  function handleJump() {
    if (jumpToPosition) {
      jumpToPosition(jumpTarget);
    }
  }
</script>

<div class="mx-auto max-w-3xl p-5">
  <h1 class="mb-2 text-center text-2xl font-bold">Virtual Scroll List Demo</h1>
  {#if isInitializing}
    <p class="mb-5 text-center text-gray-600">Initializing list items...</p>
  {:else}
    <p class="mb-5 text-center text-gray-600">Total: {totalItems} items</p>
  {/if}

  <div class="mb-5 flex items-center gap-3">
    <!-- <label for="jump-target" class="text-gray-700">Jump to position:</label> -->
    <input
      id="jump-target"
      type="number"
      bind:value={jumpTarget}
      min="0"
      max={totalItems > 0 ? totalItems - 1 : 0}
      onkeydown={(e) => {
        if (e.key === 'Enter') {
          handleJump();
        }
      }}
      class="w-full rounded-md border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
    />
    <button
      onclick={handleJump}
      class="rounded-md bg-green-500 px-4 py-2 text-white transition-colors hover:bg-green-600 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-offset-2"
    >
      Jump
    </button>
  </div>

  <div class="overflow-hidden rounded-md border border-gray-300">
    <VirtualScrollList
      {dataSource}
      {visibleItemsCount}
      itemHeight={100}
      bind:jumpToPosition
    />
  </div>
</div>
