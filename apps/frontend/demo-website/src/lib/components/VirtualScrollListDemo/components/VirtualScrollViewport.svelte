<script lang="ts">
  import { handleWheel } from '../scroll/wheel';
  import {
    handleTouchStart,
    handleTouchMove,
    handleTouchEnd,
  } from '../scroll/touch';
  import { state as listState } from '../store/list.svelte';
  import { state as scrollState, resetScroll } from '../store/scroll.svelte';
  import { state as dataState } from '../store/data.svelte';
  import { RingBufferDataWindowProvider } from '../data/DataWindowProvider';

  /**
   * VirtualScrollViewport Component
   *
   * DOM Layer Architecture:
   * -----------------------
   * This component implements a DOM recycling mechanism using a fixed number of DOM elements.
   * It maintains a visual ring buffer of DOM elements with the following characteristics:
   *
   * 1. Fixed set of DOM elements - Elements are never created/destroyed during scrolling
   * 2. Circular indexing - Elements are positioned through modulo arithmetic
   * 3. Transform-based positioning - Uses CSS transforms for performance
   *
   * The DOM layer is completely separate from the data layer. While the data layer manages
   * which items are loaded, the DOM layer handles how those items are rendered efficiently.
   */

  // Derived values
  const listItemsCount = $derived(listState.dataWindowSize);
  const viewportHeight = $derived(
    listState.itemHeight * listState.visibleItemsCount,
  );
  const listContainerHeight = $derived(listState.itemHeight * listItemsCount);

  // Initialize data window provider when data source is set
  $effect(() => {
    if (dataState.dataSource && listState.dataWindowProvider === null) {
      listState.dataWindowProvider = new RingBufferDataWindowProvider(
        dataState.dataSource,
        listState.dataWindowSize,
      );
      listState.dataWindowProvider.setDataWindowPosition(0);
      resetScroll();
    }
  });
</script>

<div
  class="overflow-hidden"
  style="height: {viewportHeight}px;"
  onwheel={handleWheel}
  ontouchstart={handleTouchStart}
  ontouchmove={handleTouchMove}
  ontouchend={handleTouchEnd}
  ontouchcancel={handleTouchEnd}
>
  <!-- 
    Container with CSS transform for smooth scrolling
    This element shifts up/down based on scroll position
  -->
  <div
    class="relative will-change-transform"
    style="height: {listContainerHeight}px; transform: translateY({scrollState.listTranslateY}px);"
  >
    <!-- 
      Render each item with absolute positioning
      The DOM elements are recycled in a circular buffer pattern
    -->
    {#each listState.dataWindowItems as item, idx (`${idx}-${item?.id}`)}
      {@const mappedItemIndex =
        (idx - scrollState.domRingHead + listItemsCount) % listItemsCount}
      {@const itemTranslateY = mappedItemIndex * listState.itemHeight}
      <div
        class="absolute w-full will-change-transform"
        style="height: {listState.itemHeight}px; transform: translateY({itemTranslateY}px);"
      >
        <div class="flex h-full items-center px-3">
          {#if item}
            <div
              class="flex flex-1 flex-col items-start rounded-lg p-2 shadow-md"
            >
              <div
                class="my-2 flex h-6 max-w-full items-center justify-center rounded-full bg-purple-600 px-4 text-xs text-white"
              >
                {item.id}
              </div>
              <div class="my-2 truncate font-medium">{item.description}</div>
            </div>
          {:else}
            <div class="flex-1 animate-pulse py-2">
              <div class="h-6 rounded bg-gray-100"></div>
            </div>
          {/if}
        </div>
      </div>
    {/each}
  </div>
</div>
