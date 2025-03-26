import type { DataItem } from '../DataSource/DataSource';

/**
 * DataWindowItem interface - represents an item that's visible in the viewport
 */
export interface DataWindowItem<T extends DataItem> {
  item: T;
  transform: string;
}

/**
 * DataWindowProvider interface
 * An abstraction for components that provide the currently loaded data window for virtual scrolling.
 * This manages a window of items loaded from the data source, which may be larger than what's
 * actually rendered on screen, providing buffer zones for smooth scrolling.
 */
export interface DataWindowProvider<T extends DataItem> {
  /**
   * The version of the data window provider.
   * This is used to announce the data source has changed, so it should be wrapped in a $state.
   */
  version: number;

  /**
   * Set the current data window position
   * This method normalizes the position to ensure it's valid
   * @param position The new position of the first item in the data window
   */
  setDataWindowPosition(position: number): void;

  /**
   * Get the list of currently loaded items in the data window
   * @param count Number of items to retrieve (optional)
   * @returns Array of data window items
   */
  getDataWindowItems(count?: number): T[];

  /**
   * Get the total number of items in the data source
   * @returns The total number of items
   */
  getTotalCount(): number;

  /**
   * Move forward by one position and return the new item
   * If at the end of the list, returns null
   * @returns The new item at the end of the data window, or null if at boundary
   */
  moveForward(): T | null;

  /**
   * Move backward by one position and return the new item
   * If at the start of the list, returns null
   * @returns The new item at the start of the data window, or null if at boundary
   */
  moveBackward(): T | null;
}
