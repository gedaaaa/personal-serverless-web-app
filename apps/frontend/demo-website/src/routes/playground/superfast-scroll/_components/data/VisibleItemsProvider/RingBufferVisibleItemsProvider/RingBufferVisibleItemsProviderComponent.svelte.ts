import type { DataItem, DataSource } from '../../DataSource/DataSource';
import { Direction } from '../../DataSource/DataSource';
import {
  CircularBidirectionalLinkedList,
  type CircularNode,
} from './CircularBidirectionalLinkedList';

import type { VisibleItemsProvider } from '../VisibleItemsProvider';

/**
 * RingBufferVisibleItemsProvider implements VisibleItemsProvider using a ring buffer
 * It provides visible items from a data source and manages data caching using a ring buffer
 * The ring buffer is organized in three sections:
 * [Lower Buffer] [Visible Items] [Upper Buffer]
 */
export class RingBufferVisibleItemsProvider<T extends DataItem>
  implements VisibleItemsProvider<T>
{
  private _versionStore = $state({
    value: 1,
  });

  get version() {
    return this._versionStore.value;
  }

  private states = {
    totalCount: 0,
  };

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

  // Debounce related properties
  private static readonly DEBOUNCE_DELAY = 16; // ~1 frame at 60fps
  private debounceTimer: number | null = null;
  private pendingUpdatePosition: number | null = null;
  private isUpdating = false; // Flag to prevent concurrent updates

  // Buffer fill throttling flags
  private isFillingUpperBuffer = false;
  private isFillingLowerBuffer = false;
  private pendingUpperBufferFill = false;
  private pendingLowerBufferFill = false;

  constructor(dataSource: DataSource<T>, visibleWindowSize: number = 10) {
    this.dataSource = dataSource;
    this.visibleWindowSize = Math.max(0, visibleWindowSize);
    this.maxBufferSize = Math.max(10, Math.min(2 * visibleWindowSize, 100));
    this.ringSize = this.visibleWindowSize + 2 * this.maxBufferSize + 1;
    this.ring = new CircularBidirectionalLinkedList<T | null>(this.ringSize);
    this.visibleWindowStart = null;
    this.visibleWindowEnd = null;
  }

  /**
   * Update the state of the visible items provider
   * This method updates the total count of items in the data source
   * and increments the version of the visible items provider
   */
  private async updateState(): Promise<void> {
    const totalCount = await this.dataSource.getTotalCount();

    this.states.totalCount = totalCount;

    this._versionStore.value++;
  }

  /**
   * Validate the position of the first visible item
   * This method clamps the position to the valid range
   * @param position The position to validate
   * @returns The clamped position
   */
  private async validatePosition(position: number): Promise<number> {
    const totalCount = await this.dataSource.getTotalCount();

    // Handle empty data source
    if (totalCount <= 0) return 0;

    // Clamp to valid range
    return Math.max(0, Math.min(position, totalCount - 1));
  }

  /**
   * Set the position of the first visible item
   * This method sets the pending update position and debounces the update
   * @param position The position to set
   */
  public setFirstVisibleItemPosition(position: number) {
    this.pendingUpdatePosition = position;

    if (this.debounceTimer !== null) {
      clearTimeout(this.debounceTimer);
    }

    if (this.visibleWindowStart === null || !this.isUpdating) {
      this.executePendingUpdatePosition();
    } else {
      this.debounceTimer = setTimeout(() => {
        this.executePendingUpdatePosition();
      }, RingBufferVisibleItemsProvider.DEBOUNCE_DELAY) as unknown as number;
    }
  }

  /**
   * Execute the pending update position
   * This method validates the position and updates the ring buffer
   */
  private async executePendingUpdatePosition(): Promise<void> {
    if (this.pendingUpdatePosition === null) {
      return;
    }

    this.isUpdating = true;
    try {
      const validatedPosition = await this.validatePosition(
        this.pendingUpdatePosition,
      );
      const newPosition = validatedPosition;
      this.pendingUpdatePosition = null;

      await this.updateRingBuffer(newPosition);
    } finally {
      this.isUpdating = false;
    }
  }

  /**
   * Get the visible items from the ring buffer
   * This method returns the visible items from the ring buffer
   * @param count The number of items to return
   * @returns The visible items
   */
  public getVisibleItems(count?: number): T[] {
    const visibleCount = count || this.visibleWindowSize;
    if (!this.visibleWindowStart) return [];
    const items = this.visibleWindowStart.getValues(visibleCount);
    return items.filter((item): item is T => item !== null);
  }

  /**
   * Get the total count of items in the data source
   * This method returns the total count of items in the data source
   * @returns The total count of items
   */
  public getTotalCount(): number {
    return this.states.totalCount;
  }

  private async updateRingBuffer(newPosition: number): Promise<void> {
    this.firstVisiblePosition = newPosition;
    await this.resetWholeRing();
    return;
  }

  /**
   * Reset the whole ring buffer
   * This method resets the ring buffer to the new position
   */
  private async resetWholeRing(): Promise<void> {
    this.ring.reset(this.ringSize);
    this.lowerBufferCount = 0;
    this.upperBufferCount = 0;

    const visibleItemsStartPos = this.firstVisiblePosition;

    let currentNode = this.ring.getHead();
    if (!currentNode) return;

    this.visibleWindowStart = currentNode;

    const visibleItems = await this.dataSource.getRangeFromId(
      visibleItemsStartPos,
      this.visibleWindowSize,
      Direction.FORWARD,
    );

    currentNode.value = visibleItems[0];

    // fill visible items
    for (let i = 1; i < visibleItems.length && currentNode; i++) {
      currentNode = currentNode.getNext();
      currentNode.value = visibleItems[i];
    }

    // set end node
    currentNode = this.visibleWindowStart;
    for (let i = 1; i < this.visibleWindowSize && currentNode; i++) {
      currentNode = currentNode.getNext();
    }
    this.visibleWindowEnd = currentNode;

    await this.updateState();

    await this.fillLowerBuffer();
    await this.fillUpperBuffer();
  }

  /**
   * Fill the lower buffer with items from the data source.
   * Loads items that come before the visible items window.
   */
  private async fillLowerBuffer(): Promise<void> {
    // If a fillLowerBuffer operation is already running, mark as pending and return
    if (this.isFillingLowerBuffer) {
      this.pendingLowerBufferFill = true;
      return;
    }

    this.isFillingLowerBuffer = true;

    try {
      if (this.visibleWindowStart?.value?.id === undefined) return;

      const itemsToLoad = this.maxBufferSize - this.lowerBufferCount + 1;

      if (itemsToLoad <= 1) return;

      let insertNode = this.visibleWindowStart;

      for (let i = 0; i < this.lowerBufferCount; i++) {
        insertNode = insertNode.getPrev();
      }

      if (insertNode.value?.id === undefined) return;

      const lowerBufferItems = await this.dataSource.getRangeFromId(
        insertNode.value?.id,
        itemsToLoad,
        Direction.BACKWARD,
      );

      for (let i = lowerBufferItems.length - 1; i > 0 && insertNode; i--) {
        insertNode = insertNode.getPrev();
        insertNode.value = lowerBufferItems[i - 1];
        this.lowerBufferCount++;
      }
    } finally {
      this.isFillingLowerBuffer = false;

      // If there's a pending request, process it
      if (this.pendingLowerBufferFill) {
        this.pendingLowerBufferFill = false;
        setTimeout(() => this.fillLowerBuffer(), 0);
      }
    }
  }

  /**
   * Fill the upper buffer with items from the data source.
   * Loads items that come after the visible items window.
   */
  private async fillUpperBuffer(): Promise<void> {
    // If a fillUpperBuffer operation is already running, mark as pending and return
    if (this.isFillingUpperBuffer) {
      this.pendingUpperBufferFill = true;
      return;
    }

    this.isFillingUpperBuffer = true;

    try {
      if (!this.visibleWindowEnd) return;

      const itemsToLoad = this.maxBufferSize - this.upperBufferCount + 1;

      if (itemsToLoad <= 1) return;

      let insertNode = this.visibleWindowEnd;

      for (let i = 1; i < this.upperBufferCount; i++) {
        insertNode = insertNode.getNext();
      }

      if (insertNode.value?.id === undefined) return;

      const upperBufferItems = await this.dataSource.getRangeFromId(
        insertNode.value?.id,
        itemsToLoad,
        Direction.FORWARD,
      );

      insertNode = insertNode.getNext();

      for (let i = 1; i < upperBufferItems.length && insertNode; i++) {
        insertNode.value = upperBufferItems[i];
        insertNode = insertNode.getNext();
        this.upperBufferCount++;
      }
    } finally {
      this.isFillingUpperBuffer = false;

      // If there's a pending request, process it
      if (this.pendingUpperBufferFill) {
        this.pendingUpperBufferFill = false;
        setTimeout(() => this.fillUpperBuffer(), 0);
      }
    }
  }

  /**
   * Advances the visible window forward by one position.
   * Returns the new item that becomes visible at the end of the window.
   *
   * @returns The new item at the end of the visible window, or null if at boundary
   */
  public moveForward(): T | null {
    // Return null if provider is not initialized
    if (!this.visibleWindowStart || !this.visibleWindowEnd) {
      return null;
    }
    // Check if upper buffer has available elements
    if (this.upperBufferCount <= 0) {
      // Buffer insufficient, trigger fill but reject movement
      setTimeout(() => {
        this.fillUpperBuffer();
      }, 0);
      return null;
    }

    // Get new element (ensure element exists)
    const nextNode = this.visibleWindowEnd.getNext();

    if (!nextNode || nextNode.value === null) {
      // If next element doesn't exist or is null, reject movement but trigger fill
      setTimeout(() => {
        this.fillUpperBuffer();
      }, 0);
      return null;
    }

    // Update firstVisiblePosition
    this.firstVisiblePosition++;

    // Move visible window pointers
    this.visibleWindowStart = this.visibleWindowStart.getNext();
    this.visibleWindowEnd = this.visibleWindowEnd.getNext();

    // Decrease upper buffer count
    this.upperBufferCount = Math.max(0, this.upperBufferCount - 1);

    // Get new element
    const newItem = this.visibleWindowEnd.value as T;

    // Asynchronously fill upper buffer if buffer ratio is below threshold
    const bufferRatio = this.upperBufferCount / this.maxBufferSize;

    if (bufferRatio < 0.5) {
      // Use setTimeout to avoid blocking UI
      setTimeout(() => {
        this.fillUpperBuffer();
      }, 0);
    }

    return newItem;
  }

  /**
   * Moves the visible window backward by one position.
   * Returns the new item that becomes visible at the start of the window.
   *
   * @returns The new item at the start of the visible window, or null if at boundary
   */
  public moveBackward(): T | null {
    // Return null if provider is not initialized
    if (!this.visibleWindowStart || !this.visibleWindowEnd) {
      return null;
    }

    // Check if lower buffer has available elements
    if (this.lowerBufferCount <= 0) {
      // Buffer insufficient, trigger fill but reject movement
      setTimeout(() => {
        this.fillLowerBuffer();
      }, 0);
      return null;
    }

    // Get previous element (ensure element exists)
    const prevNode = this.visibleWindowStart.getPrev();

    if (!prevNode || prevNode.value === null) {
      // If previous element doesn't exist or is null, reject movement but trigger fill
      setTimeout(() => {
        this.fillLowerBuffer();
      }, 0);
      return null;
    }

    // Update firstVisiblePosition
    this.firstVisiblePosition--;

    // Move visible window pointers
    this.visibleWindowStart = prevNode;
    this.visibleWindowEnd = this.visibleWindowEnd.getPrev();

    // Decrease lower buffer count
    this.lowerBufferCount = Math.max(0, this.lowerBufferCount - 1);

    // Get new element
    const newItem = this.visibleWindowStart.value as T;

    // Asynchronously fill lower buffer if buffer ratio is below threshold
    const bufferRatio = this.lowerBufferCount / this.maxBufferSize;
    if (bufferRatio < 0.5) {
      // Use setTimeout to avoid blocking UI
      setTimeout(() => {
        this.fillLowerBuffer();
      }, 0);
    }

    return newItem;
  }
}
