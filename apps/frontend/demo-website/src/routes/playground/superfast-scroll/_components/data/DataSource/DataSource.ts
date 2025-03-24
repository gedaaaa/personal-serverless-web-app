/**
 * Base data item interface that all items in the DataSource must implement
 */
export interface DataItem {
  id: number;
  [key: string]: unknown;
}

/**
 * Direction enum for data retrieval operations
 */
export enum Direction {
  FORWARD = 1, // Retrieve items with increasing ids
  BACKWARD = -1, // Retrieve items with decreasing ids
}

/**
 * DataSource interface for providing data to virtual scroll components
 * This abstraction allows different data storage implementations (SkipList, Array, API calls, etc.)
 */
export interface DataSource<T extends DataItem> {
  /**
   * Insert an item into the data source
   * @param item The item to insert
   * @returns True if the insertion was successful
   */
  insert(item: T): boolean;

  /**
   * Delete an item from the data source by id
   * @param id The id of the item to delete
   * @returns True if the deletion was successful
   */
  delete(id: number): boolean | Promise<boolean>;

  /**
   * Get a range of items starting from a specific id in the specified direction
   * @param startId The id to start from
   * @param count The number of items to retrieve
   * @param direction The direction to retrieve items in (1 for forward, -1 for backward),
   *    not affecting the order of the result set.
   * @returns Array of items
   */
  getRangeFromId(
    startId: number,
    count: number,
    direction: Direction,
  ): T[] | Promise<T[]>;

  /**
   * Get the first N items from the data source
   * @param count The number of items to retrieve
   * @returns Array of items
   */
  getFirstN(count: number): T[] | Promise<T[]>;

  /**
   * Get the total number of items in the data source
   * @returns The total number of items
   */
  getTotalCount(): number | Promise<number>;

  /**
   * Check if the position is at the start of the data
   * For non-continuous data sources, this may not be simply position === 0
   * @param position Current position
   * @returns True if at start
   */
  isAtStart(position: number): boolean | Promise<boolean>;

  /**
   * Check if the position is at the end of the data
   * For non-continuous data sources, this may not be simply position === totalCount - 1
   * @param position Current position
   * @param visibleCount Number of visible items
   * @returns True if at end
   */
  isAtEnd(position: number, visibleCount: number): boolean | Promise<boolean>;

  /**
   * Convert a position in the data to a progress value (0 to 1)
   * For non-continuous data sources, this provides accurate scrollbar positioning
   * @param position The position in the data
   * @returns A progress value between 0 and 1
   */
  getProgressForPosition(position: number): number | Promise<number>;

  /**
   * Convert a progress value (0 to 1) to a position in the data
   * This is used when the user interacts with the scrollbar
   * @param progress Progress value between 0 and 1
   * @returns The corresponding position in the data
   */
  getPositionForProgress(progress: number): number | Promise<number>;
}
