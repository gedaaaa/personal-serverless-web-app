<script lang="ts">
  import type { DataItem } from '../../data/DataSource/DataSource';
  import type { DataSource } from '../../data/DataSource/DataSource';
  import { handleScroll } from '../../utils/scrollLogic';
  import {
    RingBufferDataWindowProvider,
    type DataWindowProvider,
  } from '../../data/VisibleItemsProvider';
  import { VISUAL_BUFFER_ITEMS_COUNT } from '../../utils/types';

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

  // Props
  let {
    renderedItems = $bindable<DataItem[]>([]),
    itemHeight = 40,
    visibleItemsCount = 10,
    translateY = $bindable<number>(0),
    jumpTargetPosition = 0,
    dataSource = null,
  }: {
    renderedItems: DataItem[];
    itemHeight: number;
    visibleItemsCount: number;
    translateY: number;
    jumpTargetPosition: number | null;
    dataSource: DataSource<DataItem> | null;
  } = $props();

  let provider = $state<DataWindowProvider<DataItem> | null>(null);

  // Derived values
  const listItemsCount = visibleItemsCount + VISUAL_BUFFER_ITEMS_COUNT;
  let viewportHeight = $derived(itemHeight * visibleItemsCount);
  let listContainerHeight = $derived(itemHeight * listItemsCount);

  // DOM references
  let scrollContainer: HTMLDivElement;
  let listContainer: HTMLDivElement;

  // Touch state
  let touchStartY = $state(0);
  let lastTouchY = $state(0);
  let isTouching = $state(false);

  // DOM ring buffer index pointer
  // This tracks the current start position in the DOM ring buffer
  let domRingHead = $state(0);

  // Initialize provider when dataSource becomes available
  $effect(() => {
    if (dataSource && !provider) {
      provider = new RingBufferDataWindowProvider(dataSource, listItemsCount);

      // Set initial position
      provider.setDataWindowPosition(0);

      // Ensure initial translateY is 0
      translateY = 0;
    }
  });

  $effect(() => {
    provider?.setDataWindowPosition(jumpTargetPosition || 0);
  });

  // Track provider version for reactivity
  let version = $derived(
    (provider as DataWindowProvider<DataItem> | null)?.version,
  );
  let lastVersion = $state<number | null>(null);

  // Update UI state when provider version changes
  $effect(() => {
    if (version && version !== lastVersion) {
      renderedItems = provider!.getDataWindowItems();

      // Reset domRingHead to initial position
      domRingHead = 0;

      translateY = 0;

      lastVersion = version;
    }
  });

  /**
   * Handles mouse wheel events for smooth scrolling.
   *
   * As the user scrolls, this manages both the visual translation
   * and the shifting of data through the ring buffer when boundaries are reached.
   */
  function handleWheel(event: WheelEvent) {
    event.preventDefault();

    // Use wheel event delta for smoother scrolling experience
    // Scale the wheel delta to an appropriate scrolling distance
    const scaleFactor = 0.5;
    const delta = event.deltaY * scaleFactor;

    const result = handleScroll(
      delta > 0 ? Math.min(delta, itemHeight) : Math.max(delta, -itemHeight),
      {
        translateY,
        itemHeight,
        provider,
        visualHead: domRingHead,
        listItemsCount,
        items: renderedItems,
      },
    );

    // Update state with scroll results
    translateY = result.translateY;
    domRingHead = result.visualHead;
    renderedItems = result.items;
  }

  /**
   * Manages touch start interaction for mobile scrolling.
   */
  function handleTouchStart(event: TouchEvent) {
    if (event.touches.length === 1) {
      isTouching = true;
      touchStartY = event.touches[0].clientY;
      lastTouchY = touchStartY;
    }
  }

  /**
   * Processes touch movement to enable smooth scrolling on mobile devices.
   */
  function handleTouchMove(event: TouchEvent) {
    if (!isTouching || event.touches.length !== 1) return;

    const currentTouchY = event.touches[0].clientY;
    const deltaY = lastTouchY - currentTouchY;
    lastTouchY = currentTouchY;

    // Apply sensitivity scaling for touch scrolling
    const scaleFactor = 1;
    const scaledDelta = deltaY * scaleFactor;

    const result = handleScroll(scaledDelta, {
      translateY,
      itemHeight,
      provider,
      visualHead: domRingHead,
      listItemsCount,
      items: renderedItems,
    });

    // Update state with scroll results
    translateY = result.translateY;
    domRingHead = result.visualHead;
    renderedItems = result.items;
  }

  /**
   * Handles the end of touch interaction.
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
  <!-- 
    Container with CSS transform for smooth scrolling
    This element shifts up/down based on scroll position
  -->
  <div
    bind:this={listContainer}
    class="relative will-change-transform"
    style="height: {listContainerHeight}px; transform: translateY({translateY}px);"
  >
    <!-- 
      Render each item with absolute positioning
      The DOM elements are recycled in a circular buffer pattern
    -->
    {#each renderedItems as item, idx (`${idx}-${item?.id}`)}
      {@const mappedItemIndex =
        (idx - domRingHead + listItemsCount) % listItemsCount}
      {@const translateY = mappedItemIndex * itemHeight}
      <div
        class="absolute w-full will-change-transform"
        style="height: {itemHeight}px; transform: translateY({translateY}px);"
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
