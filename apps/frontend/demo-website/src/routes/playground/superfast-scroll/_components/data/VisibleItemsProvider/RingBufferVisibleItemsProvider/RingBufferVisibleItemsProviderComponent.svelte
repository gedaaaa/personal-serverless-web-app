<script lang="ts" generics="T extends DataItem">
  import type { DataItem, DataSource } from '../../DataSource/DataSource';
  import { Direction } from '../../DataSource/DataSource';
  import {
    CircularBidirectionalLinkedList,
    type CircularNode,
  } from './CircularBidirectionalLinkedList';

  import type {
    VisibleItemsProvider,
    VisibleItemsResult,
  } from '../VisibleItemsProvider';

  /**
   * RingBufferVisibleItemsProvider implements VisibleItemsProvider using a ring buffer
   * It provides visible items from a data source and manages data caching using a ring buffer
   * The ring buffer is organized in three sections:
   * [Lower Buffer] [Visible Items] [Upper Buffer]
   */
  export class RingBufferVisibleItemsProvider<T extends DataItem>
    implements VisibleItemsProvider<T>
  {
    private _versionStore = $state({ value: 1 }); // Using an object to wrap the version number
    private _version = 1; // Keep the original version number for internal logic

    get version() {
      return this._versionStore.value; // Access the version number through the wrapper object
    }

    // Data source
    private dataSource: DataSource<T>;

    // The visible window size - number of items visible at once
    private visibleWindowSize: number;

    // Maximum buffer size (theoretical)
    private maxBufferSize: number;

    // The ring buffer for storing items
    private ring: CircularBidirectionalLinkedList<T | null>;

    // The total size of the ring (visible items + buffers)
    private ringSize: number;

    // Current first visible item position in the dataset
    private firstVisiblePosition: number = -1;

    // Pointers to the start and end of visible window in the ring
    private visibleWindowStart: CircularNode<T | null> | null = null;
    private visibleWindowEnd: CircularNode<T | null> | null = null;

    // Actual buffer counts (may be less than maxBufferSize near data boundaries)
    private upperBufferCount: number = 0;
    private lowerBufferCount: number = 0;

    // Actual buffer sizes based on what we could load (accounts for data boundaries)
    private effectiveLowerBufferSize: number = 0;
    private effectiveUpperBufferSize: number = 0;

    // Debounce related properties
    private static readonly DEBOUNCE_DELAY = 16; // ~1 frame at 60fps
    private debounceTimer: number | null = null;
    private pendingUpdatePosition: number | null = null;
    private isUpdating = false; // Flag to prevent concurrent updates

    /**
     * Constructs a new RingBufferVisibleItemsProvider
     * @param dataSource The data source to use
     * @param visibleWindowSize Number of items visible at once
     */
    constructor(dataSource: DataSource<T>, visibleWindowSize: number = 10) {
      this.dataSource = dataSource;
      this.visibleWindowSize = visibleWindowSize;

      // Calculate max buffer size as min(2 * visibleWindowSize, 100)
      this.maxBufferSize = Math.max(10, Math.min(2 * visibleWindowSize, 100));

      // Set initial effective buffer sizes to max (will be adjusted as needed)
      this.effectiveLowerBufferSize = this.maxBufferSize;
      this.effectiveUpperBufferSize = this.maxBufferSize;

      // Calculate the total size of the ring (visible items + 2 * buffer size)
      this.ringSize = this.visibleWindowSize + 2 * this.maxBufferSize + 1; // +1 for safe insert

      // Initialize the ring buffer
      this.ring = new CircularBidirectionalLinkedList<T | null>(this.ringSize);

      // Initialize pointers to null (will be set during first reset)
      this.visibleWindowStart = null;
      this.visibleWindowEnd = null;
    }

    public forcePublishNewVersion(): void {
      console.log('forcePublishNewVersion', this._version);
      this._version++;
      this._versionStore.value = this._version; // Update the wrapper object's value
    }

    /**
     * Get the current first visible item position
     * @returns The current position
     */
    public getCurrentPosition(): number {
      return this.firstVisiblePosition;
    }

    /**
     * Get the first valid position in the data source
     * @returns The first valid position
     */
    public getFirstValidPosition(): number {
      return 0; // In most cases, 0 is the first valid position
    }

    /**
     * Get the last valid position in the data source
     * @returns The last valid position
     */
    public getLastValidPosition(): number {
      return Math.max(0, this.dataSource.getTotalCount() - 1);
    }

    /**
     * Set the first visible item position and update the data buffer
     * This method normalizes the position to ensure it's valid
     * @param position The new position
     * @returns The actual position used (after validation)
     */
    public setFirstVisibleItemPosition(position: number): number {
      // Validate and normalize the position
      const validatedPosition = this.validatePosition(position);

      // If the position hasn't changed, return directly without updating the version
      if (validatedPosition === this.firstVisiblePosition) {
        return validatedPosition;
      }

      // Store the requested position for debounced update
      this.pendingUpdatePosition = validatedPosition;

      // Cancel any existing timer
      if (this.debounceTimer !== null) {
        clearTimeout(this.debounceTimer);
      }

      // Skip debounce for first update or if not currently updating
      if (this.visibleWindowStart === null || !this.isUpdating) {
        this.executePendingUpdate();
      } else {
        // Set debounce timer
        this.debounceTimer = setTimeout(() => {
          this.executePendingUpdate();
        }, RingBufferVisibleItemsProvider.DEBOUNCE_DELAY) as unknown as number;
      }

      return validatedPosition;
    }

    /**
     * Validate and normalize a position to ensure it's within valid bounds
     * @param position The position to validate
     * @returns A valid position
     */
    private validatePosition(position: number): number {
      const totalCount = this.dataSource.getTotalCount();

      // Handle empty data source
      if (totalCount <= 0) return 0;

      // Clamp to valid range
      return Math.max(0, Math.min(position, totalCount - 1));
    }

    /**
     * Execute the pending position update
     */
    private executePendingUpdate(): void {
      // If there's no pending position, return
      if (this.pendingUpdatePosition === null) {
        return;
      }

      this.isUpdating = true;

      // Get the latest pending position
      const newPosition = this.pendingUpdatePosition;
      this.pendingUpdatePosition = null;

      // Update the ring buffer with new position
      this.updateRingBuffer(newPosition);

      this.isUpdating = false;
    }

    /**
     * Get the list of currently visible items
     * @param count Number of items to retrieve (defaults to this.visibleWindowSize)
     * @returns Array of visible items
     */
    public getVisibleItems(count?: number): T[] {
      const visibleCount = count || this.visibleWindowSize;

      // If pointers not initialized, return empty array
      if (!this.visibleWindowStart) return [];

      // Get values from the visible window
      const items = this.visibleWindowStart.getValues(visibleCount);

      const upperBufferItems = this.visibleWindowEnd?.getValues(
        this.upperBufferCount,
      );

      console.log(`Ring Buffer Report:
  First Visible Position: ${this.firstVisiblePosition}
  Visible Window Start: ${this.visibleWindowStart?.value?.id}
  Visible Window End: ${this.visibleWindowEnd?.value?.id}
  Visible Window Size: ${visibleCount}
  Total Items: ${this.dataSource.getTotalCount()}
  Buffer Items:
    Lower Buffer: ${this.lowerBufferCount}
    Upper Buffer: ${this.upperBufferCount}

    ${upperBufferItems?.map((item) => item?.id).join(', ')}
  BufferItems
  Effective Sizes:
    Lower Buffer: ${this.effectiveLowerBufferSize}
    Upper Buffer: ${this.effectiveUpperBufferSize}
      `);

      // Filter out nulls and return only valid items
      return items.filter((item): item is T => item !== null);
    }

    /**
     * Get visible items with boundary information
     * @param count Number of items to retrieve (defaults to this.visibleWindowSize)
     * @returns Object containing visible items and boundary flags
     */
    public getVisibleItemsWithBoundaryInfo(
      count?: number,
    ): VisibleItemsResult<T> {
      const items = this.getVisibleItems(count);

      // Detect boundaries using the data source's boundary detection methods if available
      let isAtStart = false;
      let isAtEnd = false;

      if (this.dataSource.isAtStart && this.dataSource.isAtEnd) {
        isAtStart = this.dataSource.isAtStart(this.firstVisiblePosition);
        isAtEnd = this.dataSource.isAtEnd(
          this.firstVisiblePosition,
          this.visibleWindowSize,
        );
      } else {
        // Fallback boundary detection
        isAtStart = this.firstVisiblePosition <= 0;
        isAtEnd =
          this.firstVisiblePosition >=
          this.dataSource.getTotalCount() - this.visibleWindowSize;
      }

      return {
        items,
        isAtStart,
        isAtEnd,
      };
    }

    /**
     * Get the total number of items in the data source
     * @returns The total number of items
     */
    public getTotalCount(): number {
      return this.dataSource.getTotalCount();
    }

    /**
     * Convert a position in the data to a progress value (0 to 1)
     * Delegates to the underlying data source for accurate mapping
     * @param position The position in the data
     * @returns A progress value between 0 and 1
     */
    public getProgressForPosition(position: number): number {
      // Prefer using the data source's mapping method if available
      if (this.dataSource.getProgressForPosition) {
        return this.dataSource.getProgressForPosition(position);
      }

      // Fallback to linear mapping
      const totalCount = this.getTotalCount();
      return totalCount > 0 ? position / (totalCount - 1) : 0;
    }

    /**
     * Convert a progress value (0 to 1) to a position in the data
     * Delegates to the underlying data source for accurate mapping
     * @param progress Progress value between 0 and 1
     * @returns The corresponding position in the data
     */
    public getPositionForProgress(progress: number): number {
      // Prefer using the data source's mapping method if available
      if (this.dataSource.getPositionForProgress) {
        return this.dataSource.getPositionForProgress(progress);
      }

      // Fallback to linear mapping
      const totalCount = this.getTotalCount();
      return Math.round(progress * (totalCount - 1));
    }

    /**
     * Update the ring buffer based on new position
     * @param newPosition The new position
     */
    private updateRingBuffer(newPosition: number): void {
      // Check if the visible window pointers are initialized
      const needsInitialization =
        !this.visibleWindowStart || !this.visibleWindowEnd;

      if (needsInitialization) {
        // Save the new position first (needed for resetBuffer)
        this.firstVisiblePosition = newPosition;

        // Initial load - reset the buffer completely
        this.resetBuffer();
        return;
      }

      // Calculate the current buffer range coverage
      const bufferStartPosition = Math.max(
        0,
        this.firstVisiblePosition - this.lowerBufferCount,
      );
      const bufferEndPosition = Math.min(
        this.firstVisiblePosition +
          this.visibleWindowSize +
          this.upperBufferCount,
        this.dataSource.getTotalCount(),
      );

      // Calculate the new visible window range
      const newVisibleEndPosition = Math.min(
        newPosition + this.visibleWindowSize,
        this.dataSource.getTotalCount(),
      );

      // Check if this is a long jump (new visible range is outside current buffer)
      const isLongJump =
        newPosition < bufferStartPosition ||
        newVisibleEndPosition > bufferEndPosition;

      if (isLongJump) {
        // Save the new position first (needed for resetBuffer)
        this.firstVisiblePosition = newPosition;

        // Long jump - reset the buffer at the new position
        this.resetBuffer();
      } else if (newPosition > this.firstVisiblePosition) {
        // Calculate scroll amount
        const scrollAmount = newPosition - this.firstVisiblePosition;

        // Update position before scrolling
        this.firstVisiblePosition = newPosition;

        // Scrolling forward
        this.scrollForward(scrollAmount);
      } else {
        // Calculate scroll amount
        const scrollAmount = this.firstVisiblePosition - newPosition;

        // Update position before scrolling
        this.firstVisiblePosition = newPosition;

        // Scrolling backward
        this.scrollBackward(scrollAmount);
      }
    }

    /**
     * Reset the buffer for the current position
     */
    private resetBuffer(): void {
      // Reset the ring
      this.ring.reset(this.ringSize);

      // Reset buffer counts and effective sizes
      this.lowerBufferCount = 0;
      this.upperBufferCount = 0;
      this.effectiveLowerBufferSize = this.maxBufferSize;
      this.effectiveUpperBufferSize = this.maxBufferSize;

      // Calculate the visible window range
      const visibleItemsStartPos = this.firstVisiblePosition;
      const visibleItemsEndPos = Math.min(
        visibleItemsStartPos + this.visibleWindowSize,
        this.dataSource.getTotalCount(),
      );
      const visibleItemsSize = visibleItemsEndPos - visibleItemsStartPos;

      // Start with the head of the ring
      let currentNode = this.ring.getHead().getNext();
      if (!currentNode) return;

      // Skip nodes for the lower buffer (we'll fill these later)
      for (let i = 0; i < this.maxBufferSize && currentNode; i++) {
        currentNode = currentNode.getNext();
      }

      // Save pointer to the start of visible window
      this.visibleWindowStart = currentNode;

      // 1. First, load visible items
      if (visibleItemsSize > 0) {
        const visibleItems = this.dataSource.getRangeFromId(
          visibleItemsStartPos,
          visibleItemsSize,
          Direction.FORWARD,
        );

        currentNode.value = visibleItems[0];

        // Insert visible items
        for (let i = 1; i < visibleItems.length && currentNode; i++) {
          currentNode = currentNode.getNext();
          currentNode.value = visibleItems[i];
        }
      }

      // Save pointer to the end of visible window (start of upper buffer)
      this.visibleWindowEnd = currentNode;

      // Update version to notify observers about visible content change
      this.forcePublishNewVersion();

      // 2. Now try to fill the lower buffer (moving backward from visible window)
      this.fillLowerBuffer();

      // 3. Then try to fill the upper buffer (moving forward from visible window)
      this.fillUpperBuffer();
    }

    /**
     * Fill the lower buffer with items
     */
    private fillLowerBuffer(): void {
      if (this.visibleWindowStart?.value?.id === undefined) return;

      // Calculate how many items we can load for the lower buffer
      const itemsToLoad =
        this.effectiveLowerBufferSize - this.lowerBufferCount + 1;

      if (itemsToLoad <= 1) return;

      // Get node that items before it should be updated
      let insertNode = this.visibleWindowStart;

      for (let i = 0; i < this.lowerBufferCount; i++) {
        insertNode = insertNode.getPrev();
      }

      if (insertNode.value?.id === undefined) return;

      // Load items for lower buffer
      const lowerBufferItems = this.dataSource.getRangeFromId(
        insertNode.value?.id,
        itemsToLoad,
        Direction.BACKWARD,
      );

      // Insert items in reverse order (so they appear in correct order in the buffer)\
      for (let i = lowerBufferItems.length - 1; i > 0 && insertNode; i--) {
        insertNode = insertNode.getPrev();
        insertNode.value = lowerBufferItems[i - 1];
        this.lowerBufferCount++;
      }

      // If we couldn't load the full buffer, adjust the effective size
      this.effectiveLowerBufferSize = Math.max(this.lowerBufferCount, 1);
    }

    /**
     * Fill the upper buffer with items
     */
    private fillUpperBuffer(): void {
      if (!this.visibleWindowEnd) return;

      // Calculate how many items we can load for the upper buffer

      const itemsToLoad =
        this.effectiveUpperBufferSize - this.upperBufferCount + 1;

      console.log(
        `fillUpperBuffer: itemsToLoad: ${itemsToLoad}`,
        this.effectiveUpperBufferSize,
        this.upperBufferCount,
      );

      if (itemsToLoad <= 1) return;

      // Get node that items before it should be updated
      let insertNode = this.visibleWindowEnd;

      console.log(
        `fillUpperBuffer: inital insertNode: ${insertNode.value?.id}`,
      );

      for (let i = 1; i < this.upperBufferCount; i++) {
        console.log(`fillUpperBuffer: insertNode: ${insertNode.value?.id}`);
        insertNode = insertNode.getNext();
      }

      console.log(`fillUpperBuffer: final insertNode: ${insertNode.value?.id}`);

      if (insertNode.value?.id === undefined) return;

      console.log(
        `fillUpperBuffer: insertNode.value?.id: ${insertNode.value?.id}`,
      );

      // Load items for upper buffer
      const upperBufferItems = this.dataSource.getRangeFromId(
        insertNode.value?.id,
        itemsToLoad,
        Direction.FORWARD,
      );

      console.log(
        `fillUpperBuffer: upperBufferItems: ${upperBufferItems.map((item) => item?.id).join(', ')}, ${upperBufferItems.length}`,
      );

      insertNode = insertNode.getNext();

      // Insert items
      for (let i = 1; i < upperBufferItems.length && insertNode; i++) {
        insertNode.value = upperBufferItems[i];
        insertNode = insertNode.getNext();
        this.upperBufferCount++;
      }

      console.log(
        `fillUpperBuffer: final upperBufferCount: ${this.upperBufferCount}`,
      );

      // If we couldn't load the full buffer, adjust the effective size
      this.effectiveUpperBufferSize = Math.max(this.upperBufferCount, 1);
    }

    /**
     * Scroll forward by updating the ring buffer
     * @param count Number of positions to scroll forward
     */
    private scrollForward(count: number): void {
      if (!this.visibleWindowStart || !this.visibleWindowEnd) {
        this.resetBuffer();
        return;
      }

      // Update the pointers by moving them forward
      for (let i = 0; i < count; i++) {
        if (this.visibleWindowStart) {
          this.visibleWindowStart = this.visibleWindowStart.getNext();
        }
        if (this.visibleWindowEnd) {
          this.visibleWindowEnd = this.visibleWindowEnd.getNext();
        }
      }

      this.upperBufferCount -= count;
      this.upperBufferCount = Math.max(0, this.upperBufferCount);

      // Update version to notify observers about visible content change
      this.forcePublishNewVersion();

      // Refill buffer only if we're below 50% of capacity
      const bufferRatio = this.upperBufferCount / this.effectiveUpperBufferSize;
      if (bufferRatio < 0.5) {
        this.fillUpperBuffer();
      }
    }

    /**
     * Scroll backward by updating the ring buffer
     * @param count Number of positions to scroll backward
     */
    private scrollBackward(count: number): void {
      if (!this.visibleWindowStart || !this.visibleWindowEnd) {
        this.resetBuffer();
        return;
      }

      // Update the pointers by moving them backward
      for (let i = 0; i < count; i++) {
        if (this.visibleWindowStart) {
          this.visibleWindowStart = this.visibleWindowStart.getPrev();
        }
        if (this.visibleWindowEnd) {
          this.visibleWindowEnd = this.visibleWindowEnd.getPrev();
        }
      }

      this.lowerBufferCount -= count;
      this.lowerBufferCount = Math.max(0, this.lowerBufferCount);

      // Update version to notify observers about visible content change
      this.forcePublishNewVersion();

      // Refill buffer only if we're below 50% of capacity
      const bufferRatio = this.lowerBufferCount / this.effectiveLowerBufferSize;

      if (bufferRatio < 0.5) {
        this.fillLowerBuffer();
      }
    }
  }
  // Using rune syntax to define props
  let {
    provider = $bindable<RingBufferVisibleItemsProvider<T>>(),
    dataSource,
    itemsWindowSize = 10,
  }: {
    provider: RingBufferVisibleItemsProvider<T> | null;
    dataSource: DataSource<T> | null;
    itemsWindowSize?: number;
  } = $props();

  // instance the provider
  $effect(() => {
    if (dataSource) {
      provider = new RingBufferVisibleItemsProvider(
        dataSource,
        itemsWindowSize,
      );
    }
  });
</script>
