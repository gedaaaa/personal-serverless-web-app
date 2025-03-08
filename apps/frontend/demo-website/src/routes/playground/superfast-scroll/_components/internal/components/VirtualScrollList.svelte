<script lang="ts" generics="T extends DataItem">
  import type { DataItem, DataSource } from '../../data/DataSource/DataSource';
  import { type RingBufferVisibleItemsProvider } from '../../data/VisibleItemsProvider/RingBufferVisibleItemsProvider';
  import VirtualScrollViewport from './VirtualScrollViewport.svelte';
  import VirtualScrollControls from './VirtualScrollControls.svelte';
  import { createJumpToPositionHandler } from '../../utils/scrollLogic';
  import {
    DEFAULT_ITEM_HEIGHT,
    DEFAULT_VISIBLE_ITEMS_COUNT,
  } from '../../utils/types';

  // Props definition
  let {
    provider = $bindable<RingBufferVisibleItemsProvider<T>>(),
    dataSource,
    itemHeight = DEFAULT_ITEM_HEIGHT,
    visibleItemsCount = DEFAULT_VISIBLE_ITEMS_COUNT,
    jumpToPosition = $bindable<(position: number) => void>(),
  }: {
    provider: RingBufferVisibleItemsProvider<T> | null;
    dataSource: DataSource<T> | null;
    itemHeight?: number;
    visibleItemsCount?: number;
    jumpToPosition?: (position: number) => void;
  } = $props();

  // State
  let currentPosition: number | null = $state(null);
  let items = $state<T[]>([]);
  let totalCount = $state(0);
  let isAtStart = $state(true);
  let isAtEnd = $state(false);
  let translateY = $state(0);

  // Track provider version for reactivity
  $effect(() => {
    if (provider) {
      totalCount = provider.getTotalCount();
    }
  });

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
    bind:provider
    {dataSource}
  />

  <VirtualScrollControls
    currentPosition={currentPosition || 0}
    {totalCount}
    {isAtStart}
    {isAtEnd}
    bind:provider
    onJumpToPosition={handleJumpToPosition}
  />
</div>
