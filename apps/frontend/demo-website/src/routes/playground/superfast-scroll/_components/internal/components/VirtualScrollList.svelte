<script lang="ts">
  import type { DataItem, DataSource } from '../../data/DataSource/DataSource';
  import VirtualScrollViewport from './VirtualScrollViewport.svelte';
  import VirtualScrollControls from './VirtualScrollControls.svelte';
  import { createJumpToPositionHandler } from '../../utils/scrollLogic';
  import {
    DEFAULT_ITEM_HEIGHT,
    DEFAULT_VISIBLE_ITEMS_COUNT,
  } from '../../utils/types';
  import type { VisibleItemsProvider } from '../../data';

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
  let currentPosition: number | null = $state(null);
  let items = $state<DataItem[]>([]);
  let totalCount = $state(0);
  let isAtStart = $state(true);
  let isAtEnd = $state(false);
  let translateY = $state(0);

  // Create and bind the jump to position handler
  const handleJumpToPosition = createJumpToPositionHandler(
    (position) => {
      currentPosition = position;
    },
    (newTranslateY) => {
      translateY = newTranslateY;
    },
  );

  // Bind the jumpToPosition function for external use
  $effect(() => {
    jumpToPosition = handleJumpToPosition;
  });
</script>

<div
  class="flex h-full touch-none flex-col overflow-hidden rounded-md border border-gray-200"
>
  <VirtualScrollViewport
    bind:items
    {itemHeight}
    {visibleItemsCount}
    bind:translateY
    bind:currentPosition
    bind:isAtStart
    bind:isAtEnd
    {dataSource}
  />

  <VirtualScrollControls
    currentPosition={currentPosition || 0}
    {totalCount}
    {isAtStart}
    {isAtEnd}
    onJumpToPosition={handleJumpToPosition}
  />
</div>
