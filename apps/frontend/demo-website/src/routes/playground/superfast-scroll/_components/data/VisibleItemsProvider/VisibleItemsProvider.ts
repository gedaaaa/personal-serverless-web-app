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

  forcePublishNewVersion(): void;

  /**
   * Set the current first visible item position
   * This method normalizes the position to ensure it's valid
   * @param position The new position of the first visible item
   * @returns The actual position used (which may be different if the requested position was invalid)
   */
  setFirstVisibleItemPosition(position: number): number;

  /**
   * Get the list of currently visible items
   * @param count Number of items to retrieve (optional)
   * @returns Array of visible items
   */
  getVisibleItems(count?: number): T[];

  /**
   * Get visible items with boundary information
   * This enhanced version simplifies boundary handling in virtual scrolling components
   * @param count Number of items to retrieve (optional)
   * @returns Object containing visible items and boundary flags
   */
  getVisibleItemsWithBoundaryInfo(count?: number): VisibleItemsResult<T>;

  /**
   * Get the total number of items in the data source
   * @returns The total number of items
   */
  getTotalCount(): number;

  /**
   * Get the first valid position
   * @returns The first valid position in the data source
   */
  getFirstValidPosition(): number;

  /**
   * Get the last valid position
   * @returns The last valid position in the data source
   */
  getLastValidPosition(): number;

  /**
   * Convert a position in the data to a progress value (0 to 1)
   * This method delegates to the data source for accurate mapping
   * @param position The position in the data
   * @returns A progress value between 0 and 1
   */
  getProgressForPosition(position: number): number;

  /**
   * Convert a progress value (0 to 1) to a position in the data
   * This method delegates to the data source for accurate mapping
   * @param progress Progress value between 0 and 1
   * @returns The corresponding position in the data
   */
  getPositionForProgress(progress: number): number;
}
