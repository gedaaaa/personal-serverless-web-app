<script lang="ts">
  import type { DataItem } from '../../data/DataSource/DataSource';
  import type { DataSource } from '../../data/DataSource/DataSource';
  import { handleScroll } from '../../utils/scrollLogic';
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
    jumpTargetPosition = 0,
    dataSource = null,
  }: {
    items: DataItem[];
    itemHeight: number;
    visibleItemsCount: number;
    translateY: number;
    jumpTargetPosition: number | null;
    dataSource: DataSource<DataItem> | null;
  } = $props();

  let provider = $state<VisibleItemsProvider<DataItem> | null>(null);

  // Derived values
  const listItemsCount = visibleItemsCount + BUFFER_ITEMS_COUNT;
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

  // Initialize provider when dataSource becomes available
  $effect(() => {
    if (dataSource && !provider) {
      provider = new RingBufferVisibleItemsProvider(dataSource, listItemsCount);

      // Set initial position
      provider.setFirstVisibleItemPosition(0);

      // Ensure initial translateY is 0
      translateY = 0;
    }
  });

  $effect(() => {
    provider?.setFirstVisibleItemPosition(jumpTargetPosition || 0);
  });

  // Track provider version for reactivity
  let version = $derived(
    (provider as VisibleItemsProvider<DataItem> | null)?.version,
  );
  let lastVersion = $state<number | null>(null);

  // Update UI state when provider version changes
  $effect(() => {
    if (version && version !== lastVersion) {
      items = provider!.getVisibleItems();

      // Reset visualHead to initial position
      visualHead = 0;

      translateY = 0;

      lastVersion = version;
    }
  });

  /**
   * Handles mouse wheel events for smooth scrolling.
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
        visualHead,
        listItemsCount,
        items,
      },
    );

    // Update state with scroll results
    translateY = result.translateY;
    visualHead = result.visualHead;
    items = result.items;
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
      visualHead,
      listItemsCount,
      items,
    });

    // Update state with scroll results
    translateY = result.translateY;
    visualHead = result.visualHead;
    items = result.items;
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
  <div
    bind:this={listContainer}
    class="relative will-change-transform"
    style="height: {listContainerHeight}px; transform: translateY({translateY}px);"
  >
    {#each items as item, idx (`${idx}-${item?.id}`)}
      {@const mappedItemIndex =
        (idx - visualHead + listItemsCount) % listItemsCount}
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
