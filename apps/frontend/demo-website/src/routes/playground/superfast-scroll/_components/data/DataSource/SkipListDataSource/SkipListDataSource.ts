import type { DataItem, DataSource } from '../DataSource';
import { Direction } from '../DataSource';
import { SkipList, type SkipListItem } from './SkipList';

/**
 * Implementation of DataSource using SkipList as the underlying data structure
 */
export class SkipListDataSource<T extends DataItem> implements DataSource<T> {
  private skipList: SkipList<SkipListItem>;
  private totalItems: number = 0;

  /**
   * Constructs a new SkipListDataSource
   */
  constructor() {
    this.skipList = new SkipList<SkipListItem>();
  }

  /**
   * Get the total number of items
   * @returns The total number of items
   */
  getTotalCount(): number {
    return this.totalItems;
  }

  /**
   * Insert an item into the SkipList
   * @param item The item to insert
   * @returns True if the insertion was successful
   */
  insert(item: T): boolean {
    const skipListItem: SkipListItem = {
      id: item.id,
      data: item,
    };

    this.skipList.insert(skipListItem);

    // Update total count if this is a new item with a higher id
    if (item.id >= this.totalItems) {
      this.totalItems = item.id + 1;
    }

    return true;
  }

  /**
   * Delete an item from the SkipList by id
   * @param id The id of the item to delete
   * @returns True if the deletion was successful
   */
  delete(id: number): boolean {
    const result = this.skipList.remove(id);
    return result !== null;
  }

  /**
   * Get a range of items starting from a specific id in the specified direction
   * @param startId The id to start from
   * @param count The number of items to retrieve
   * @param direction The direction to retrieve items (FORWARD or BACKWARD)
   * @returns Array of items
   */
  getRangeFromId(startId: number, count: number, direction: Direction): T[] {
    // Validate inputs
    if (count <= 0 || startId < 0 || startId >= this.totalItems) {
      return [];
    }

    // Use the skipList's built-in getRangeFromId for efficient retrieval
    // This directly leverages the skiplist's bidirectional traversal capabilities
    const skipListItems = this.skipList.getRangeFromId(
      startId,
      direction as 1 | -1, // Convert Direction enum to 1 | -1 for SkipList
      count,
    );

    // Convert SkipListItems to T items
    const items = skipListItems
      .filter((item) => item && item.data)
      .map((item) => item.data as T);

    // When direction is -1 (BACKWARD), reverse the result list
    // This ensures the returned list is always sorted by ID from small to large
    return direction === Direction.BACKWARD ? items.reverse() : items;
  }
}
