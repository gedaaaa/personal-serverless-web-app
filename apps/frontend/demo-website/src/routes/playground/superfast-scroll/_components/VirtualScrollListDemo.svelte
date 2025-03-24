<script lang="ts">
  import { onMount } from 'svelte';
  import VirtualScrollList from './internal/components/VirtualScrollList.svelte';
  import type { DataItem, DataSource } from './data/DataSource/DataSource';
  import type { VisibleItemsProvider } from './data/VisibleItemsProvider/VisibleItemsProvider';
  import { DelayedDataSource } from './data/DataSource/DelayedDataSource';

  // Define a simple data item type
  interface DemoItem extends DataItem {
    id: number;
    name: string;
    description: string;
  }

  // Create data source and provider
  let dataSource: DataSource<DemoItem> | null = $state(null);
  let jumpToPosition: (position: number) => void = $state(() => {});
  let jumpTarget = $state(0);
  let totalItems = $state(0);

  const visibleItemsCount = 10;

  // Initialize data source
  onMount(() => {
    const initialDataSource = new DelayedDataSource<DemoItem>();
    // Generate and insert 1000000 demo items
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
  });

  // Function to handle jump button click
  function handleJump() {
    if (jumpToPosition) {
      jumpToPosition(jumpTarget);
    }
  }
</script>

<div class="mx-auto max-w-3xl p-5">
  <h1 class="mb-2 text-center text-2xl font-bold">Virtual Scroll List Demo</h1>
  <p class="mb-5 text-center text-gray-600">Total: {totalItems} items</p>

  <div class="mb-5 flex items-center gap-3">
    <label for="jump-target" class="text-gray-700">Jump to position:</label>
    <input
      id="jump-target"
      type="number"
      bind:value={jumpTarget}
      min="0"
      max={totalItems > 0 ? totalItems - 1 : 0}
      class="rounded-md border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
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
