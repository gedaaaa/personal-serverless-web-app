<script lang="ts">
  import type { DataItem, DataSource } from '../../data/DataSource/DataSource';
  import VirtualScrollViewport from './VirtualScrollViewport.svelte';
  import { setDataSource } from '../store/data.svelte';
  import { setItemHeight, setVisibleItemsCount } from '../store/list.svelte';
  import registerJumpReactive from '../jump/registerJumpReactive.svelte';

  /**
   * Component properties for configuring the virtual scroll list.
   */
  let {
    dataSource,
    itemHeight,
    visibleItemsCount,
  }: {
    dataSource: DataSource<DataItem> | null;
    itemHeight?: number;
    visibleItemsCount?: number;
  } = $props();

  // set state based on props
  $effect(() => setDataSource(dataSource));
  $effect(() => setItemHeight(itemHeight ?? 0));
  $effect(() => setVisibleItemsCount(visibleItemsCount ?? 0));

  registerJumpReactive();
</script>

<div
  class="flex h-full touch-none flex-col overflow-hidden rounded-md border border-gray-200"
>
  <VirtualScrollViewport />
</div>
