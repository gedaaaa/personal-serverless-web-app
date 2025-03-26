import type { DataItem, DataSource } from '../../DataSource/DataSource';
import { Direction } from '../../DataSource/DataSource';
import {
  CircularBidirectionalLinkedList,
  type CircularNode,
} from './CircularBidirectionalLinkedList';

import type { DataWindowProvider } from '../DataWindowProvider';

/**
 * RingBufferDataWindowProvider implements DataWindowProvider using a ring buffer.
 *
 * Data Layer Architecture:
 * -----------------------
 * This provider manages a three-segment ring buffer for data:
 * [Lower Buffer] [Data Window] [Upper Buffer]
 *
 * - Data Window: The main section containing items currently in view or potentially about to be in view
 * - Lower Buffer: Pre-loaded items before the current data window
 * - Upper Buffer: Pre-loaded items after the current data window
 *
 * The buffer segments are dynamically loaded as the user scrolls, creating a
 * smooth experience even with large datasets.
 */
export class RingBufferDataWindowProvider<T extends DataItem>
  implements DataWindowProvider<T>
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

  // The data window size - number of items loaded at once
  private dataWindowSize: number;

  // Maximum buffer size (theoretical)
  private maxBufferSize: number;

  // The ring buffer for storing items
  private ring: CircularBidirectionalLinkedList<T | null>;

  // The total size of the ring (data window + buffers)
  private ringSize: number;

  // Current first position in the dataset
  private firstDataPosition: number = -1;

  // Pointers to the start and end of data window in the ring
  private dataWindowStart: CircularNode<T | null> | null = null;
  private dataWindowEnd: CircularNode<T | null> | null = null;

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

  constructor(dataSource: DataSource<T>, dataWindowSize: number = 10) {
    this.dataSource = dataSource;
    this.dataWindowSize = Math.max(0, dataWindowSize);
    this.maxBufferSize = Math.max(10, Math.min(2 * dataWindowSize, 100));
    this.ringSize = this.dataWindowSize + 2 * this.maxBufferSize + 1;
    this.ring = new CircularBidirectionalLinkedList<T | null>(this.ringSize);
    this.dataWindowStart = null;
    this.dataWindowEnd = null;
  }

  /**
   * Update the state of the data window provider
   * This method updates the total count of items in the data source
   * and increments the version of the data window provider
   */
  private async updateState(): Promise<void> {
    const totalCount = await this.dataSource.getTotalCount();

    this.states.totalCount = totalCount;

    this._versionStore.value++;
  }

  /**
   * Validate the position of the first item in the data window
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
   * Set the position of the first item in the data window
   * This method sets the pending update position and debounces the update
   * @param position The position to set
   */
  public setDataWindowPosition(position: number) {
    this.pendingUpdatePosition = position;

    if (this.debounceTimer !== null) {
      clearTimeout(this.debounceTimer);
    }

    if (this.dataWindowStart === null || !this.isUpdating) {
      this.executePendingUpdatePosition();
    } else {
      this.debounceTimer = setTimeout(() => {
        this.executePendingUpdatePosition();
      }, RingBufferDataWindowProvider.DEBOUNCE_DELAY) as unknown as number;
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
   * Get the items from the data window
   * This method returns the items currently in the data window
   * @param count The number of items to return
   * @returns The data window items
   */
  public getDataWindowItems(count?: number): T[] {
    const dataCount = count || this.dataWindowSize;
    if (!this.dataWindowStart) return [];
    const items = this.dataWindowStart.getValues(dataCount);
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
    this.firstDataPosition = newPosition;
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

    const dataItemsStartPos = this.firstDataPosition;

    let currentNode = this.ring.getHead();
    if (!currentNode) return;

    this.dataWindowStart = currentNode;

    const dataItems = await this.dataSource.getRangeFromId(
      dataItemsStartPos,
      this.dataWindowSize,
      Direction.FORWARD,
    );

    currentNode.value = dataItems[0];

    // fill data window items
    for (let i = 1; i < dataItems.length && currentNode; i++) {
      currentNode = currentNode.getNext();
      currentNode.value = dataItems[i];
    }

    // set end node
    currentNode = this.dataWindowStart;
    for (let i = 1; i < this.dataWindowSize && currentNode; i++) {
      currentNode = currentNode.getNext();
    }
    this.dataWindowEnd = currentNode;

    await this.updateState();

    await this.fillLowerBuffer();
    await this.fillUpperBuffer();
  }

  // Implementation for moveForward and moveBackward methods
  // These methods provide navigation through the data window

  /**
   * Move the data window forward by one item
   * Returns the new item that should enter the window from below
   */
  public moveForward(): T | null {
    if (!this.dataWindowStart || !this.dataWindowEnd) return null;

    // Check if we have an item in the upper buffer
    if (this.upperBufferCount <= 0) return null;

    // Move data window pointers
    this.dataWindowStart = this.dataWindowStart.getNext();
    this.dataWindowEnd = this.dataWindowEnd.getNext();

    // Update buffer counts
    this.lowerBufferCount++;
    this.upperBufferCount--;

    // Schedule buffer refill if needed
    this.scheduleBufferFill();

    return this.dataWindowEnd.value;
  }

  /**
   * Move the data window backward by one item
   * Returns the new item that should enter the window from above
   */
  public moveBackward(): T | null {
    if (!this.dataWindowStart || !this.dataWindowEnd) return null;

    // Check if we have an item in the lower buffer
    if (this.lowerBufferCount <= 0) return null;

    // Move data window pointers
    this.dataWindowStart = this.dataWindowStart.getPrev();
    this.dataWindowEnd = this.dataWindowEnd.getPrev();

    // Update buffer counts
    this.lowerBufferCount--;
    this.upperBufferCount++;

    // Schedule buffer refill if needed
    this.scheduleBufferFill();

    return this.dataWindowStart.value;
  }

  /**
   * Schedule buffer fills if buffers are getting low
   */
  private scheduleBufferFill(): void {
    if (this.lowerBufferCount < this.maxBufferSize / 2) {
      this.fillLowerBuffer();
    }

    if (this.upperBufferCount < this.maxBufferSize / 2) {
      this.fillUpperBuffer();
    }
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
      if (this.dataWindowStart?.value?.id === undefined) return;

      const itemsToLoad = this.maxBufferSize - this.lowerBufferCount + 1;

      if (itemsToLoad <= 1) return;

      let insertNode = this.dataWindowStart;

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
      if (!this.dataWindowEnd) return;

      const itemsToLoad = this.maxBufferSize - this.upperBufferCount + 1;

      if (itemsToLoad <= 1) return;

      let insertNode = this.dataWindowEnd;

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
}
