<script lang="ts">
  import type { DataItem } from '../../data/DataSource/DataSource';
  import type { DataSource } from '../../data/DataSource/DataSource';
  import {
    handleWheelScroll,
    handleTouchScroll,
  } from '../../utils/scrollLogic';
  import {
    RingBufferVisibleItemsProvider,
    type VisibleItemsProvider,
  } from '../../data/VisibleItemsProvider';
  import { BUFFER_ITEMS_COUNT } from '../../utils/types';

  // Props
  let {
    items = $bindable<DataItem[]>([]),
    itemHeight = 40,
    visibleItemsCount = 10,
    translateY = $bindable<number>(0),
    currentPosition = $bindable<number | null>(null),
    isAtStart = $bindable(false),
    isAtEnd = $bindable(false),
    provider = $bindable<VisibleItemsProvider<DataItem> | null>(null),
    dataSource = $bindable<DataSource<DataItem> | null>(null),
  }: {
    items: DataItem[];
    itemHeight: number;
    visibleItemsCount: number;
    translateY: number;
    currentPosition: number | null;
    isAtStart: boolean;
    isAtEnd: boolean;
    provider: VisibleItemsProvider<DataItem> | null;
    dataSource: DataSource<DataItem> | null;
  } = $props();

  // Derived values
  const listItemsCount = visibleItemsCount + BUFFER_ITEMS_COUNT; // 2 extra items for buffer
  let viewportHeight = $derived(itemHeight * visibleItemsCount);
  let listContainerHeight = $derived(itemHeight * listItemsCount);

  // DOM references
  let scrollContainer: HTMLDivElement;
  let listContainer: HTMLDivElement;

  // Touch state
  let touchStartY = $state(0);
  let lastTouchY = $state(0);
  let isTouching = $state(false);
  let visualHead = $state(0);
  let tempVisualHead = 0;

  // Initialize provider when dataSource becomes available
  $effect(() => {
    if (dataSource) {
      provider = new RingBufferVisibleItemsProvider(
        dataSource,
        visibleItemsCount,
      );
      // Initialize provider with current position
      if (currentPosition === null) {
        currentPosition = 0;
      }
    }
  });

  // Track provider version for reactivity
  let version = $derived(provider?.version);

  // Update UI state when provider version changes
  $effect(() => {
    // If you check the rendering of list items,
    //  you will find that it depends on both virtualRingHead and items.
    // We use calculated mapped index for each list item DOM element,
    //  to control the visual order of them.
    // So we need to update virtualRingHead and items at same tick,
    //  otherwise the DOM will render twice and causing re-render.
    if (version && provider) {
      const result = provider.getVisibleItemsWithBoundaryInfo();
      visualHead = (tempVisualHead + listItemsCount) % listItemsCount;
      items = result.items;
      isAtStart = result.isAtStart;
      isAtEnd = result.isAtEnd;
    }
  });

  // Update position in provider when currentPosition changes
  $effect(() => {
    if (provider && currentPosition !== null) {
      provider.setFirstVisibleItemPosition(currentPosition);
    }
  });

  /**
   * Handle wheel events for scrolling
   */
  function handleWheel(event: WheelEvent) {
    const result = handleWheelScroll(event, {
      translateY,
      currentPosition,
      isAtStart,
      isAtEnd,
      itemHeight,
      provider,
      tempVirtualRingHead: tempVisualHead,
    });

    translateY = result.translateY;
    currentPosition = result.currentPosition;
    tempVisualHead = result.tempVirtualRingHead;
  }

  /**
   * Handle touch start event
   */
  function handleTouchStart(event: TouchEvent) {
    if (event.touches.length === 1) {
      isTouching = true;
      touchStartY = event.touches[0].clientY;
      lastTouchY = touchStartY;
    }
  }

  /**
   * Handle touch move event
   */
  function handleTouchMove(event: TouchEvent) {
    if (!isTouching || event.touches.length !== 1) return;

    const currentTouchY = event.touches[0].clientY;
    const deltaY = lastTouchY - currentTouchY;
    lastTouchY = currentTouchY;

    const result = handleTouchScroll(deltaY, {
      translateY,
      currentPosition,
      isAtStart,
      isAtEnd,
      itemHeight,
      provider,
      tempVirtualRingHead: tempVisualHead,
    });

    translateY = result.translateY;
    currentPosition = result.currentPosition;
    tempVisualHead = result.tempVirtualRingHead;
  }

  /**
   * Handle touch end event
   */
  function handleTouchEnd() {
    isTouching = false;
  }
</script>

<div
  bind:this={scrollContainer}
  class="overflow-hidden"
  style="height: {viewportHeight}px;"
  onwheel={handleWheel}
  ontouchstart={handleTouchStart}
  ontouchmove={handleTouchMove}
  ontouchend={handleTouchEnd}
  ontouchcancel={handleTouchEnd}
>
  <div
    bind:this={listContainer}
    class="relative will-change-transform"
    style="height: {listContainerHeight}px; transform: translateY({translateY}px);"
  >
    <!-- eslint-disable-next-line @typescript-eslint/no-unused-vars -->
    {#each { length: listItemsCount } as _, idx}
      <!--
        We use calculated mapped index for each list item DOM element,
        to control the visual order of them.
      -->
      {@const mappedItemIndex =
        (idx - visualHead + listItemsCount) % listItemsCount}
      {@const translateY = mappedItemIndex * itemHeight}
      {@const item = items[mappedItemIndex]}
      <div
        class="absolute box-border w-full border-b border-gray-100 will-change-transform"
        style="height: {itemHeight}px; transform: translateY({translateY}px);"
      >
        <div class="flex h-full items-center px-3">
          <span class="text-gray-700">Item ID: {item?.id}</span>
        </div>
      </div>
    {/each}
  </div>
</div>
