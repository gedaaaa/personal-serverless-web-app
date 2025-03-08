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

  /**
   * Get the first N items from the data source
   * @param count The number of items to retrieve
   * @returns Array of items
   */
  getFirstN(count: number): T[] {
    // If there are no items or count is invalid, return empty array
    if (count <= 0 || this.totalItems === 0) {
      return [];
    }

    // Get items starting from ID 0 in forward direction
    return this.getRangeFromId(0, count, Direction.FORWARD);
  }

  /**
   * Get the underlying SkipList instance
   * @returns The SkipList instance
   */
  getSkipList(): SkipList<SkipListItem> {
    return this.skipList;
  }

  /**
   * Check if the position is at the start of the data
   * For SkipList, this checks if we're at the first valid item
   * @param position Current position
   * @returns True if at start
   */
  isAtStart(position: number): boolean {
    // For non-continuous data, we might need to get the actual first item ID
    // In this implementation, we assume 0 is the first valid position
    return position <= 0;
  }

  /**
   * Check if the position is at the end of the data
   * For SkipList, this checks if we're at or near the last valid item
   * @param position Current position
   * @param visibleCount Number of visible items
   * @returns True if at end
   */
  isAtEnd(position: number, visibleCount: number): boolean {
    // For continuous data, it's simple
    // But for non-continuous data, we might need to check if there are any more
    // valid items after the current position + visibleCount

    // First, check if we're close to the end based on total count
    if (position >= this.totalItems - visibleCount) {
      return true;
    }

    // Then, check if there are actually any items in the range after the visible area
    // This handles sparse data sources where not all positions have items
    const nextItems = this.getRangeFromId(
      position + visibleCount,
      1,
      Direction.FORWARD,
    );

    return nextItems.length === 0;
  }

  /**
   * Convert a position to a scrollbar progress value (0-1)
   * Uses skip list levels as a sampling mechanism for non-continuous data
   * @param position The position to convert
   * @returns Progress value between 0 and 1
   */
  getProgressForPosition(position: number): number {
    // For empty data source or invalid position
    if (this.totalItems <= 0 || position < 0) {
      return 0;
    }

    // Access the underlying skip list
    const skipList = this.skipList;
    const skipListSize = skipList.getTotalCount();

    // If the skip list is empty, can't determine progress
    if (skipListSize === 0) {
      return 0;
    }

    // Check if position is beyond the highest ID we have
    const lastItem = skipList.getLast();
    if (lastItem && position >= lastItem.id) {
      return 1;
    }

    // Check if position is before the lowest ID we have
    const firstItem = skipList.getFirst();
    if (firstItem && position <= firstItem.id) {
      return 0;
    }

    // Choose a sampling level:
    // For small lists, use level 0 (all nodes)
    // For larger lists, use higher levels for sparser sampling
    // Note: Level 0 has all nodes, Level 1 has ~1/2 nodes, Level 2 has ~1/4 nodes, etc.
    const maxLevel = skipList.getMaxLevel();
    const samplingLevel = Math.min(
      Math.floor(Math.log2(skipListSize) / 2), // Using log base 2 for better matching with skip list probability
      Math.max(0, maxLevel - 1), // Cap at maxLevel-1 or 0 if maxLevel is 0
    );

    // Get nodes at the chosen level
    const samplingNodes = skipList.getNodesAtLevel(samplingLevel);

    // If no nodes at this level, fall back to level 0 (all nodes)
    if (samplingNodes.length === 0 && samplingLevel > 0) {
      const allNodes = skipList.getNodesAtLevel(0);
      if (allNodes.length === 0) {
        return 0; // Empty list
      }

      // Use all nodes for sampling
      return this.calculateProgressFromSamples(position, allNodes);
    }

    // Calculate progress using the sampling nodes
    return this.calculateProgressFromSamples(position, samplingNodes);
  }

  /**
   * Helper method to calculate progress from a list of sampling nodes
   * @param position The position to convert
   * @param samplingNodes The nodes to use for sampling
   * @returns Progress value between 0 and 1
   */
  private calculateProgressFromSamples(
    position: number,
    samplingNodes: SkipListItem[],
  ): number {
    // Check again based on our collected samples
    if (samplingNodes.length === 0) {
      return 0;
    }

    if (position <= samplingNodes[0].id) {
      return 0;
    }

    if (position >= samplingNodes[samplingNodes.length - 1].id) {
      return 1;
    }

    // Binary search to find the closest sampling points
    let left = 0;
    let right = samplingNodes.length - 1;

    // Find the largest id that's less than or equal to position
    while (left < right) {
      const mid = Math.floor((left + right + 1) / 2);
      if (samplingNodes[mid].id <= position) {
        left = mid;
      } else {
        right = mid - 1;
      }
    }

    // Found exact match
    if (samplingNodes[left].id === position) {
      return left / (samplingNodes.length - 1);
    }

    // Find the next valid id after position
    let rightIndex = left + 1;
    while (
      rightIndex < samplingNodes.length &&
      samplingNodes[rightIndex].id <= position
    ) {
      rightIndex++;
    }

    // If no next valid id found
    if (rightIndex >= samplingNodes.length) {
      return 1;
    }

    // Linear interpolation between the two sampling points
    const leftId = samplingNodes[left].id;
    const rightId = samplingNodes[rightIndex].id;
    const leftProgress = left / (samplingNodes.length - 1);
    const rightProgress = rightIndex / (samplingNodes.length - 1);

    // Interpolate between the points
    return (
      leftProgress +
      ((position - leftId) / (rightId - leftId)) *
        (rightProgress - leftProgress)
    );
  }

  /**
   * Convert a scrollbar progress value (0-1) to a position
   * Uses skip list levels as a sampling mechanism for non-continuous data
   * @param progress Progress value between 0 and 1
   * @returns The corresponding position in the data
   */
  getPositionForProgress(progress: number): number {
    // Validate progress value
    if (progress <= 0) return 0;
    if (progress >= 1) return this.totalItems - 1;
    if (this.totalItems <= 1) return 0;

    // Access the underlying skip list
    const skipList = this.skipList;
    const skipListSize = skipList.getTotalCount();

    // If the skip list is empty or has few items, fall back to linear mapping
    if (skipListSize <= 1) {
      return Math.round(progress * (this.totalItems - 1));
    }

    // Get all items at the chosen level
    const items = this.getFirstN(skipListSize);

    // If no items, fall back to linear mapping
    if (items.length === 0) {
      return Math.round(progress * (this.totalItems - 1));
    }

    // Convert progress to an index in our sampling array
    const exactIndex = progress * (items.length - 1);
    const leftIndex = Math.floor(exactIndex);
    const rightIndex = Math.ceil(exactIndex);

    // Handle edge cases
    if (leftIndex === rightIndex) {
      return items[leftIndex].id;
    }

    // Linear interpolation between the two closest sampling points
    const leftProgress = leftIndex / (items.length - 1);
    const rightProgress = rightIndex / (items.length - 1);
    const progressRatio =
      (progress - leftProgress) / (rightProgress - leftProgress);

    const leftPosition = items[leftIndex].id;
    const rightPosition = items[rightIndex].id;

    // Interpolate to get the actual position
    return Math.round(
      leftPosition + progressRatio * (rightPosition - leftPosition),
    );
  }
}
