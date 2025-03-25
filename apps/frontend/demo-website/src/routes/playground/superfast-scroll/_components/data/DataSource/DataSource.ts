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
   * Get the total number of items in the data source
   * @returns The total number of items
   */
  getTotalCount(): number | Promise<number>;
}
