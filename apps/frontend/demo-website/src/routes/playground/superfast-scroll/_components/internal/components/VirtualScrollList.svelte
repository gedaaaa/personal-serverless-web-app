<script lang="ts">
  import type { DataItem, DataSource } from '../../data/DataSource/DataSource';
  import VirtualScrollViewport from './VirtualScrollViewport.svelte';
  import {
    DEFAULT_ITEM_HEIGHT,
    DEFAULT_VISIBLE_ITEMS_COUNT,
  } from '../../utils/types';

  // Props definition
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

  // State
  let jumpTargetPosition: number | null = $state(null);
  let items = $state<DataItem[]>([]);
  let translateY = $state(0);

  // Bind the jumpToPosition function for external use
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
    bind:items
    {jumpTargetPosition}
    {dataSource}
  />
</div>
