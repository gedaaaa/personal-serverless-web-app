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
  private _versionStore = $state({
    value: 1,
  });

  get version() {
    return this._versionStore.value;
  }

  private states = {
    isAtStart: false,
    isAtEnd: false,
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

  // Actual buffer sizes based on what we could load (accounts for data boundaries)
  private effectiveLowerBufferSize: number = 0;
  private effectiveUpperBufferSize: number = 0;

  // Debounce related properties
  private static readonly DEBOUNCE_DELAY = 16; // ~1 frame at 60fps
  private debounceTimer: number | null = null;
  private pendingUpdatePosition: number | null = null;
  private isUpdating = false; // Flag to prevent concurrent updates

  constructor(dataSource: DataSource<T>, visibleWindowSize: number = 10) {
    this.dataSource = dataSource;
    this.visibleWindowSize = visibleWindowSize;
    this.maxBufferSize = Math.max(10, Math.min(2 * visibleWindowSize, 100));
    this.effectiveLowerBufferSize = this.maxBufferSize;
    this.effectiveUpperBufferSize = this.maxBufferSize;
    this.ringSize = this.visibleWindowSize + 2 * this.maxBufferSize + 1;
    this.ring = new CircularBidirectionalLinkedList<T | null>(this.ringSize);
    this.visibleWindowStart = null;
    this.visibleWindowEnd = null;
  }

  private async updateState(): Promise<void> {
    const [isAtStart, isAtEnd, totalCount] = await Promise.all([
      this.dataSource.isAtStart(this.firstVisiblePosition),
      this.dataSource.isAtEnd(
        this.firstVisiblePosition,
        this.visibleWindowSize,
      ),
      this.dataSource.getTotalCount(),
    ]);

    this.states.isAtStart = isAtStart;
    this.states.isAtEnd = isAtEnd;
    this.states.totalCount = totalCount;

    this._versionStore.value++;

    console.log(
      'updateState',
      this._versionStore.value,
      this.firstVisiblePosition,
      this.visibleWindowStart?.value?.id,
    );
  }

  private async validatePosition(position: number): Promise<number> {
    const totalCount = await this.dataSource.getTotalCount();

    // Handle empty data source
    if (totalCount <= 0) return 0;

    // Clamp to valid range
    return Math.max(0, Math.min(position, totalCount - 1));
  }

  public setFirstVisibleItemPosition(position: number) {
    this.pendingUpdatePosition = position;

    if (this.debounceTimer !== null) {
      clearTimeout(this.debounceTimer);
    }

    if (this.visibleWindowStart === null || !this.isUpdating) {
      this.executePendingUpdate();
    } else {
      this.debounceTimer = setTimeout(() => {
        this.executePendingUpdate();
      }, RingBufferVisibleItemsProvider.DEBOUNCE_DELAY) as unknown as number;
    }
  }

  private async executePendingUpdate(): Promise<void> {
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

  public getVisibleItems(count?: number): T[] {
    console.log('getVisibleItems', count, this.visibleWindowStart);
    const visibleCount = count || this.visibleWindowSize;
    if (!this.visibleWindowStart) return [];
    const items = this.visibleWindowStart.getValues(visibleCount);
    return items.filter((item): item is T => item !== null);
  }

  public getVisibleItemsWithBoundaryInfo(
    count?: number,
  ): VisibleItemsResult<T> {
    const items = this.getVisibleItems(count);
    return {
      items,
      isAtStart: this.states.isAtStart,
      isAtEnd: this.states.isAtEnd,
    };
  }

  public getTotalCount(): number {
    return this.states.totalCount;
  }

  public getProgressForPosition(position: number): number {
    return 0;
  }

  public getPositionForProgress(progress: number): number {
    return 0;
  }

  private async updateRingBuffer(newPosition: number): Promise<void> {
    const needsInitialization =
      !this.visibleWindowStart || !this.visibleWindowEnd;
    console.log(
      'needsInitialization',
      newPosition,
      needsInitialization,
      this.visibleWindowStart,
      this.visibleWindowEnd,
    );

    if (needsInitialization) {
      this.firstVisiblePosition = newPosition;
      await this.resetBuffer();
      return;
    }

    const bufferStartPosition = Math.max(
      0,
      this.firstVisiblePosition - this.lowerBufferCount,
    );
    const bufferEndPosition = Math.min(
      this.firstVisiblePosition +
        this.visibleWindowSize +
        this.upperBufferCount,
      await this.dataSource.getTotalCount(),
    );

    const newVisibleEndPosition = Math.min(
      newPosition + this.visibleWindowSize,
      await this.dataSource.getTotalCount(),
    );

    const isLongJump =
      newPosition < bufferStartPosition ||
      newVisibleEndPosition > bufferEndPosition;

    if (isLongJump) {
      this.firstVisiblePosition = newPosition;
      await this.resetBuffer();
    } else if (newPosition > this.firstVisiblePosition) {
      const scrollAmount = newPosition - this.firstVisiblePosition;
      this.firstVisiblePosition = newPosition;
      await this.scrollForward(scrollAmount);
    } else {
      const scrollAmount = this.firstVisiblePosition - newPosition;
      this.firstVisiblePosition = newPosition;
      await this.scrollBackward(scrollAmount);
    }
  }

  private async resetBuffer(): Promise<void> {
    console.log('resetBuffer', this.ringSize);
    this.ring.reset(this.ringSize);
    this.lowerBufferCount = 0;
    this.upperBufferCount = 0;
    this.effectiveLowerBufferSize = this.maxBufferSize;
    this.effectiveUpperBufferSize = this.maxBufferSize;

    const totalCount = await this.dataSource.getTotalCount();
    const visibleItemsStartPos = this.firstVisiblePosition;
    const visibleItemsEndPos = Math.min(
      visibleItemsStartPos + this.visibleWindowSize,
      totalCount,
    );
    const visibleItemsSize = visibleItemsEndPos - visibleItemsStartPos;

    let currentNode = this.ring.getHead().getNext();
    if (!currentNode) return;

    for (let i = 0; i < this.maxBufferSize && currentNode; i++) {
      currentNode = currentNode.getNext();
    }

    this.visibleWindowStart = currentNode;

    console.log('resetBuffer', this.visibleWindowStart);
    if (visibleItemsSize > 0) {
      const visibleItems = await this.dataSource.getRangeFromId(
        visibleItemsStartPos,
        visibleItemsSize,
        Direction.FORWARD,
      );

      currentNode.value = visibleItems[0];

      for (let i = 1; i < visibleItems.length && currentNode; i++) {
        currentNode = currentNode.getNext();
        currentNode.value = visibleItems[i];
      }
    }

    this.visibleWindowEnd = currentNode;

    await this.updateState();

    await this.fillLowerBuffer();
    await this.fillUpperBuffer();
  }

  private async fillLowerBuffer(): Promise<void> {
    if (this.visibleWindowStart?.value?.id === undefined) return;

    const itemsToLoad =
      this.effectiveLowerBufferSize - this.lowerBufferCount + 1;

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

    this.effectiveLowerBufferSize = Math.max(this.lowerBufferCount, 1);
  }

  private async fillUpperBuffer(): Promise<void> {
    if (!this.visibleWindowEnd) return;

    const itemsToLoad =
      this.effectiveUpperBufferSize - this.upperBufferCount + 1;

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

    this.effectiveUpperBufferSize = Math.max(this.upperBufferCount, 1);
  }

  private async scrollForward(count: number): Promise<void> {
    if (!this.visibleWindowStart || !this.visibleWindowEnd) {
      await this.resetBuffer();
      return;
    }

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

    await this.updateState();

    const bufferRatio = this.upperBufferCount / this.effectiveUpperBufferSize;
    if (bufferRatio < 0.5) {
      await this.fillUpperBuffer();
    }
  }

  private async scrollBackward(count: number): Promise<void> {
    if (!this.visibleWindowStart || !this.visibleWindowEnd) {
      await this.resetBuffer();
      return;
    }

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

    await this.updateState();

    const bufferRatio = this.lowerBufferCount / this.effectiveLowerBufferSize;
    if (bufferRatio < 0.5) {
      await this.fillLowerBuffer();
    }
  }

  public forcePublishNewVersion(): void {
    this._versionStore.value++;
  }
}
