import type { DataItem } from '../DataSource/DataSource';

/**
 * VisibleItem interface - represents an item that's visible in the viewport
 */
export interface VisibleItem<T extends DataItem> {
  item: T;
  transform: string;
}

/**
 * VisibleItemsResult interface - represents the result of getting visible items
 * Includes boundary information to simplify virtual scrolling components
 */
export interface VisibleItemsResult<T extends DataItem> {
  items: T[];
  isAtStart: boolean;
  isAtEnd: boolean;
}

/**
 * VisibleItemsProvider interface
 * An abstraction for components that provide visible items for virtual scrolling
 */
export interface VisibleItemsProvider<T extends DataItem> {
  /**
   * The version of the visible items provider.
   * This is used to anounce the data source has changed, so it should be wrapped in a $state.
   */
  version: number;

  /**
   * Set the current first visible item position
   * This method normalizes the position to ensure it's valid
   * @param position The new position of the first visible item
   */
  setFirstVisibleItemPosition(position: number): void;

  /**
   * Get the list of currently visible items
   * @param count Number of items to retrieve (optional)
   * @returns Array of visible items
   */
  getVisibleItems(count?: number): T[];

  /**
   * Get the total number of items in the data source
   * @returns The total number of items
   */
  getTotalCount(): number;

  /**
   * Move forward by one position and return the new item
   * If at the end of the list, returns null
   * @returns The new item at the end of the visible window, or null if at boundary
   */
  moveForward(): T | null;

  /**
   * Move backward by one position and return the new item
   * If at the start of the list, returns null
   * @returns The new item at the start of the visible window, or null if at boundary
   */
  moveBackward(): T | null;
}
