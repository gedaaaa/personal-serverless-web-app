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
   *
   * Behavior details:
   * - If startId exists in the data source, items will be retrieved starting from that item
   * - If startId does not exist in the data source:
   *   - For FORWARD direction: Return items starting from the first item with id > startId
   *   - For BACKWARD direction: Return items starting from the last item with id < startId
   * - If no suitable items exist after applying these rules, return an empty array
   * - The order of returned items remains consistent (ascending/descending by id depending on implementation),
   *   regardless of the direction parameter (which only indicates the retrieval direction)
   * - Maximum count items will be returned, or fewer if not enough items are available
   *
   * @param startId The id to start retrieving from
   * @param count The maximum number of items to retrieve
   * @param direction The direction to retrieve items in
   * @returns Array of items that match the criteria
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
