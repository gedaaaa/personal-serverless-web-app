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
    // provider = $bindable<VisibleItemsProvider<DataItem> | null>(null),
    dataSource = null,
  }: {
    items: DataItem[];
    itemHeight: number;
    visibleItemsCount: number;
    translateY: number;
    currentPosition: number | null;
    isAtStart: boolean;
    isAtEnd: boolean;
    // provider: VisibleItemsProvider<DataItem> | null;
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
  let provider: VisibleItemsProvider<DataItem> | null = $state(null);

  // 滚动校正状态
  let pendingBounceCorrection = $state(false); // 是否有待处理的边界校正
  let unboundedTranslateY = $state(0); // 不受边界限制的translateY
  let targetTranslateY = $state(0); // 校正后的目标translateY
  let isPositionUpdating = $state(false); // 防止连续触发position更新
  let translateYOffset = $state(0); // 保存translateY的小数部分偏移

  // 累积滚动状态
  let accumulatedScrollDeltaY = $state(0); // 累积的滚动像素

  // Initialize provider when dataSource becomes available
  $effect(() => {
    if (dataSource && !provider) {
      provider = new RingBufferVisibleItemsProvider(dataSource, listItemsCount);

      // Initialize provider with current position
      if (currentPosition === null) {
        currentPosition = 0;
      }

      // 初始化自由滚动值
      unboundedTranslateY = translateY;
      targetTranslateY = translateY;
      translateYOffset = 0;
      accumulatedScrollDeltaY = 0;
    }
  });

  // Track provider version for reactivity
  let version = $derived(provider?.version);
  let lastVersion = $state<number | null>(null);

  // Update UI state when provider version changes
  $effect(() => {
    // If you check the rendering of list items,
    //  you will find that it depends on both virtualRingHead and items.
    // We use calculated mapped index for each list item DOM element,
    //  to control the visual order of them.
    // So we need to update virtualRingHead and items at same tick,
    //  otherwise the DOM will render twice and causing re-render.
    if (version && version !== lastVersion) {
      const result = provider!.getVisibleItemsWithBoundaryInfo();
      // console.log('updateUIState', version, result);
      visualHead = (tempVisualHead + listItemsCount) % listItemsCount;
      items = result.items;
      isAtStart = result.isAtStart;
      isAtEnd = result.isAtEnd;

      // 数据更新后，处理待校正的边界弹跳
      if (pendingBounceCorrection) {
        // 计算主体位置 (targetTranslateY) 与内部偏移量 (translateYOffset)
        const basePosition =
          Math.floor(targetTranslateY / itemHeight) * itemHeight;
        const newTranslateY = basePosition + translateYOffset;

        translateY = newTranslateY;
        unboundedTranslateY = newTranslateY; // 同步无边界值
        pendingBounceCorrection = false;
      }
      lastVersion = version;

      // 重置位置更新锁定状态
      isPositionUpdating = false;
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
    // 计算无边界限制的translateY
    unboundedTranslateY = unboundedTranslateY - event.deltaY;

    // 保存内部偏移量 (在一个项目高度内的偏移)
    translateYOffset = unboundedTranslateY % itemHeight;

    // 累积滚动量
    accumulatedScrollDeltaY += event.deltaY;

    // 计算这个滚动可能导致的position变化
    const potentialPositionDelta = Math.floor(
      Math.abs(accumulatedScrollDeltaY) / itemHeight,
    );

    // 检查边界条件
    let shouldProcessScroll = true;

    // 在边界位置应用限制
    if ((isAtStart && event.deltaY < 0) || (isAtEnd && event.deltaY > 0)) {
      // 在边界处，根据方向应用限制
      if (isAtStart && event.deltaY < 0) {
        // 在起始位置向上滚动，限制translateY为0
        unboundedTranslateY = 0;
        targetTranslateY = 0;
        translateY = 0;
        // 重置累积量防止越过边界
        accumulatedScrollDeltaY = 0;
      } else if (isAtEnd && event.deltaY > 0) {
        // 在结束位置向下滚动，限制translateY为-2*itemHeight
        unboundedTranslateY = -2 * itemHeight;
        targetTranslateY = -2 * itemHeight;
        translateY = -2 * itemHeight;
        // 重置累积量防止越过边界
        accumulatedScrollDeltaY = 0;
      }
      shouldProcessScroll = false;
    }

    // 只有在非边界位置或向有效方向滚动时，才处理滚动逻辑
    if (shouldProcessScroll) {
      // 如果正在等待边界校正或位置更新锁定中，但累积的滚动量足够大时仍进行更新
      if (
        (pendingBounceCorrection || isPositionUpdating) &&
        potentialPositionDelta >= 1
      ) {
        // 强制进行一次更新
        const result = handleWheelScroll(event, {
          translateY,
          currentPosition,
          isAtStart,
          isAtEnd,
          itemHeight,
          provider,
          tempVirtualRingHead: tempVisualHead,
        });

        // 记录正确的目标translateY（有边界校正的值）
        targetTranslateY = result.translateY;

        // 检查是否需要边界校正
        if (unboundedTranslateY !== targetTranslateY) {
          pendingBounceCorrection = true;
        }

        // 检查position是否变化
        const positionChanged = result.currentPosition !== currentPosition;

        // 更新位置和临时头指针
        currentPosition = result.currentPosition;
        tempVisualHead = result.tempVirtualRingHead;

        // 重置累积量
        accumulatedScrollDeltaY = 0;
      } else if (!(pendingBounceCorrection || isPositionUpdating)) {
        // 正常处理（未锁定状态）
        const result = handleWheelScroll(event, {
          translateY,
          currentPosition,
          isAtStart,
          isAtEnd,
          itemHeight,
          provider,
          tempVirtualRingHead: tempVisualHead,
        });

        // 记录正确的目标translateY（有边界校正的值）
        targetTranslateY = result.translateY;

        // 检查是否需要边界校正
        if (unboundedTranslateY !== targetTranslateY) {
          pendingBounceCorrection = true;
        }

        // 检查position是否变化
        const positionChanged = result.currentPosition !== currentPosition;

        // 更新位置和临时头指针
        currentPosition = result.currentPosition;
        tempVisualHead = result.tempVirtualRingHead;

        // 如果位置发生变化，启动防抖锁定
        if (positionChanged) {
          isPositionUpdating = true;
          // 重置累积量
          accumulatedScrollDeltaY = 0;
        }
      }

      // 视觉上使用无边界的translateY，让滚动看起来流畅
      translateY = unboundedTranslateY;
    }
  }

  /**
   * Handle touch start event
   */
  function handleTouchStart(event: TouchEvent) {
    if (event.touches.length === 1) {
      isTouching = true;
      touchStartY = event.touches[0].clientY;
      lastTouchY = touchStartY;
      // 重置累积滚动量
      accumulatedScrollDeltaY = 0;
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

    // 计算无边界限制的translateY
    unboundedTranslateY = unboundedTranslateY - deltaY;

    // 保存内部偏移量 (在一个项目高度内的偏移)
    translateYOffset = unboundedTranslateY % itemHeight;

    // 累积滚动量
    accumulatedScrollDeltaY += deltaY;

    // 计算这个滚动可能导致的position变化
    const potentialPositionDelta = Math.floor(
      Math.abs(accumulatedScrollDeltaY) / itemHeight,
    );

    // 检查边界条件
    let shouldProcessScroll = true;

    // 在边界位置应用限制
    if ((isAtStart && deltaY < 0) || (isAtEnd && deltaY > 0)) {
      // 在边界处，根据方向应用限制
      if (isAtStart && deltaY < 0) {
        // 在起始位置向上滚动（手指向下移动），限制translateY为0
        unboundedTranslateY = 0;
        targetTranslateY = 0;
        translateY = 0;
        // 重置累积量防止越过边界
        accumulatedScrollDeltaY = 0;
      } else if (isAtEnd && deltaY > 0) {
        // 在结束位置向下滚动（手指向上移动），限制translateY为-2*itemHeight
        unboundedTranslateY = -2 * itemHeight;
        targetTranslateY = -2 * itemHeight;
        translateY = -2 * itemHeight;
        // 重置累积量防止越过边界
        accumulatedScrollDeltaY = 0;
      }
      shouldProcessScroll = false;
    }

    // 只有在非边界位置或向有效方向滚动时，才处理滚动逻辑
    if (shouldProcessScroll) {
      // 如果正在等待边界校正或位置更新锁定中，但累积的滚动量足够大时仍进行更新
      if (
        (pendingBounceCorrection || isPositionUpdating) &&
        potentialPositionDelta >= 1
      ) {
        // 强制进行一次更新
        const result = handleTouchScroll(deltaY, {
          translateY,
          currentPosition,
          isAtStart,
          isAtEnd,
          itemHeight,
          provider,
          tempVirtualRingHead: tempVisualHead,
        });

        // 记录正确的目标translateY（有边界校正的值）
        targetTranslateY = result.translateY;

        // 检查是否需要边界校正
        if (unboundedTranslateY !== targetTranslateY) {
          pendingBounceCorrection = true;
        }

        // 检查position是否变化
        const positionChanged = result.currentPosition !== currentPosition;

        // 更新位置和临时头指针
        currentPosition = result.currentPosition;
        tempVisualHead = result.tempVirtualRingHead;

        // 重置累积量
        accumulatedScrollDeltaY = 0;
      } else if (!(pendingBounceCorrection || isPositionUpdating)) {
        const result = handleTouchScroll(deltaY, {
          translateY,
          currentPosition,
          isAtStart,
          isAtEnd,
          itemHeight,
          provider,
          tempVirtualRingHead: tempVisualHead,
        });

        // 记录正确的目标translateY（有边界校正的值）
        targetTranslateY = result.translateY;

        // 检查是否需要边界校正
        if (unboundedTranslateY !== targetTranslateY) {
          pendingBounceCorrection = true;
        }

        // 检查position是否变化
        const positionChanged = result.currentPosition !== currentPosition;

        // 更新位置和临时头指针
        currentPosition = result.currentPosition;
        tempVisualHead = result.tempVirtualRingHead;

        // 如果位置发生变化，启动防抖锁定
        if (positionChanged) {
          isPositionUpdating = true;
          // 重置累积量
          accumulatedScrollDeltaY = 0;
        }
      }

      // 视觉上使用无边界的translateY，让滚动看起来流畅
      translateY = unboundedTranslateY;
    }
  }

  /**
   * Handle touch end event
   */
  function handleTouchEnd() {
    isTouching = false;
    // 重置累积滚动量
    accumulatedScrollDeltaY = 0;
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
      <!-- {@debug translateY, visualHead, item, idx, mappedItemIndex} -->
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
