<script lang="ts">
  import { onMount } from 'svelte';
  import type { DataItem, DataSource } from './data/DataSource/DataSource';
  import { DelayedDataSource } from './data/DataSource/DelayedDataSource';
  import { setJumpTargetPosition } from './store/data.svelte';
  import VirtualScrollList from './components/VirtualScrollList.svelte';
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
  let jumpTarget = $state(0);
  let totalItems = $state(0);
  let isInitializing = $state(true);

  const totalItemsToGenerate = 1000000;
  const batchSize = 1000;
  /**
   * Initializes the data source with a large set of demo items.
   * Creates a million items and makes them available for the virtual scroll list.
   */
  onMount(() => {
    isInitializing = true;
    const initialDataSource = new DelayedDataSource<DemoItem>();

    // Process items in batches
    const processBatch = (startIndex: number) => {
      // Process current batch
      const endIndex = Math.min(startIndex + batchSize, totalItemsToGenerate);

      for (let i = startIndex; i < endIndex; i++) {
        const item: DemoItem = {
          id: i,
          name: `Item ${i}`,
          description: `This is the description for item ${i}`,
        };
        initialDataSource.insert(item);
      }

      // Schedule next batch if needed
      if (endIndex < totalItemsToGenerate) {
        setTimeout(() => processBatch(endIndex), 0);
        Promise.resolve(initialDataSource.getTotalCount()).then((count) => {
          totalItems = count;
        });
        if (startIndex === 0) {
          dataSource = initialDataSource;
          isInitializing = false;
        }
      }
    };

    // Start processing batches
    processBatch(0);
  });
</script>

<div class="mx-auto max-w-3xl p-5">
  <h1 class="mb-2 text-center text-2xl font-bold">Virtual Scroll List Demo</h1>
  {#if isInitializing}
    <p class="mb-5 text-center text-gray-600">Initializing list items...</p>
  {:else}
    <p class="mb-5 text-center text-gray-600">Total: {totalItems} items</p>
  {/if}

  <div class="mb-5 flex items-center gap-3">
    <label for="jump-target" class="text-gray-700">Jump to position:</label>
    <input
      id="jump-target"
      type="number"
      bind:value={jumpTarget}
      min="0"
      onkeydown={(e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          setJumpTargetPosition(jumpTarget);
        }
      }}
      class="w-full rounded-md border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
    />
    <button
      onclick={() => setJumpTargetPosition(jumpTarget)}
      class="rounded-md bg-purple-600 px-4 py-2 text-white transition-colors"
    >
      Jump
    </button>
  </div>

  <div class="overflow-hidden rounded-md border border-gray-300">
    <VirtualScrollList {dataSource} visibleItemsCount={10} itemHeight={100} />
  </div>
</div>
