<script lang="ts">
  import type { DataItem, DataSource } from '../../data/DataSource/DataSource';
  import VirtualScrollViewport from './VirtualScrollViewport.svelte';
  import {
    DEFAULT_ITEM_HEIGHT,
    DEFAULT_VISIBLE_ITEMS_COUNT,
  } from '../../utils/types';

  /**
   * Component properties for configuring the virtual scroll list.
   */
  let {
    dataSource,
    itemHeight = DEFAULT_ITEM_HEIGHT,
    visibleItemsCount = DEFAULT_VISIBLE_ITEMS_COUNT,
    jumpToPosition = $bindable<(position: number) => void>(),
  }: {
    dataSource: DataSource<DataItem> | null;
    itemHeight?: number;
    visibleItemsCount?: number;
    jumpToPosition?: (position: number) => void;
  } = $props();

  // Internal state management
  let jumpTargetPosition: number | null = $state(null);
  let renderedItems = $state<DataItem[]>([]);
  let translateY = $state(0);

  /**
   * Exposes a jumpToPosition function for external components to control scrolling.
   * When called, this function will reset the translate position and jump to the specified item.
   */
  $effect(() => {
    jumpToPosition = (position: number) => {
      jumpTargetPosition = position;
      translateY = 0;
    };
  });
</script>

<div
  class="flex h-full touch-none flex-col overflow-hidden rounded-md border border-gray-200"
>
  <VirtualScrollViewport
    {itemHeight}
    {visibleItemsCount}
    bind:translateY
    bind:renderedItems
    {jumpTargetPosition}
    {dataSource}
  />
</div>
