/**
 * SkipList implementation
 * A bidirectional probabilistic data structure with O(log n) search/insert/delete operations
 * and O(1) traversal in both directions
 */

// Maximum level for this skip list
const MAX_LEVEL = 16;

// Probability factor for level promotion (p = 1/PROBABILITY_FACTOR)
const PROBABILITY_FACTOR = 2;

interface SkipNode<T> {
  // The node's value
  value: T;
  // Array of forward pointers (next nodes at each level)
  forward: Array<SkipNode<T> | null>;
  // Array of backward pointers (previous nodes at each level)
  backward: Array<SkipNode<T> | null>;
}

export interface SkipListItem {
  id: number;
  data?: Record<string, unknown>;
}

export class SkipList<T extends SkipListItem> {
  // Header node (sentinel)
  private header: SkipNode<T>;
  // Tail node (sentinel)
  private tail: SkipNode<T>;
  // Current max level of skip list
  private level: number;
  // Element counter
  private size: number;

  constructor() {
    this.header = {
      value: null as unknown as T,
      forward: Array(MAX_LEVEL).fill(null),
      backward: Array(MAX_LEVEL).fill(null),
    };

    this.tail = {
      value: null as unknown as T,
      forward: Array(MAX_LEVEL).fill(null),
      backward: Array(MAX_LEVEL).fill(null),
    };

    // Initialize: header points forward to tail, tail points backward to header
    for (let i = 0; i < MAX_LEVEL; i++) {
      this.header.forward[i] = this.tail;
      this.tail.backward[i] = this.header;
    }

    this.level = 0;
    this.size = 0;
  }

  /**
   * Generates a random level for node insertion
   * with decreasing probability for each level
   */
  private randomLevel(): number {
    let level = 0;
    while (Math.random() < 1 / PROBABILITY_FACTOR && level < MAX_LEVEL - 1) {
      level++;
    }
    return level;
  }

  /**
   * Search for a node with specific ID
   * @param id ID to search for
   * @returns The node or null if not found
   */
  public search(id: number): T | null {
    let current = this.header;

    // Start from the highest level and work down
    for (let i = this.level; i >= 0; i--) {
      while (
        current.forward[i] !== this.tail &&
        current.forward[i]!.value.id < id
      ) {
        current = current.forward[i]!;
      }
    }

    // Move to the next node (level 0)
    current = current.forward[0]!;

    // Check if current node has the target id (and is not the tail)
    if (current !== this.tail && current.value.id === id) {
      return current.value;
    }

    return null;
  }

  /**
   * Insert a new item into the skip list
   * @param item Item to insert
   */
  public insert(item: T): void {
    const update: Array<SkipNode<T>> = Array(MAX_LEVEL).fill(this.header);
    let current = this.header;

    // Find position to insert
    for (let i = this.level; i >= 0; i--) {
      while (
        current.forward[i] !== this.tail &&
        current.forward[i]!.value.id < item.id
      ) {
        current = current.forward[i]!;
      }
      update[i] = current;
    }

    // Next node at level 0
    current = current.forward[0]!;

    // Check if we're updating an existing node
    if (current !== this.tail && current.value.id === item.id) {
      current.value = item;
      return;
    }

    // Generate a random level for the new node
    const newLevel = this.randomLevel();

    // Update skip list level if needed
    if (newLevel > this.level) {
      for (let i = this.level + 1; i <= newLevel; i++) {
        update[i] = this.header;
      }
      this.level = newLevel;
    }

    // Create new node
    const newNode: SkipNode<T> = {
      value: item,
      forward: Array(newLevel + 1).fill(null),
      backward: Array(newLevel + 1).fill(null),
    };

    // Insert node at appropriate level
    for (let i = 0; i <= newLevel; i++) {
      // Set forward pointers
      newNode.forward[i] = update[i].forward[i];
      update[i].forward[i] = newNode;

      // Set backward pointers
      newNode.backward[i] = update[i];
      if (newNode.forward[i] !== null) {
        newNode.forward[i]!.backward[i] = newNode;
      }
    }

    this.size++;
  }

  /**
   * Remove a node with the given ID
   * @param id ID to remove
   * @returns Removed item or null if not found
   */
  public remove(id: number): T | null {
    const update: Array<SkipNode<T>> = Array(MAX_LEVEL).fill(this.header);
    let current = this.header;

    // Find position to delete
    for (let i = this.level; i >= 0; i--) {
      while (
        current.forward[i] !== this.tail &&
        current.forward[i]!.value.id < id
      ) {
        current = current.forward[i]!;
      }
      update[i] = current;
    }

    // Get the node to delete
    current = current.forward[0]!;

    // If node exists and has the target id
    if (current !== this.tail && current.value.id === id) {
      const removedValue = current.value;

      // Remove references to the node at all levels
      for (let i = 0; i <= this.level; i++) {
        if (update[i].forward[i] !== current) {
          break;
        }

        // Update forward pointers
        update[i].forward[i] = current.forward[i];

        // Update backward pointers
        if (current.forward[i] !== null) {
          current.forward[i]!.backward[i] = update[i];
        }
      }

      // Update the level of the skip list if needed
      while (this.level > 0 && this.header.forward[this.level] === this.tail) {
        this.level--;
      }

      this.size--;
      return removedValue;
    }

    return null;
  }

  /**
   * Get a range of items starting from a specific ID in the specified direction
   * @param startId ID to start from
   * @param direction Direction (1 for forward, -1 for backward)
   * @param count Number of items to retrieve
   * @returns Array of items in the range
   */
  public getRangeFromId(
    startId: number,
    direction: 1 | -1,
    count: number,
  ): T[] {
    const result: T[] = [];
    if (this.size === 0 || count <= 0) {
      return result;
    }

    // Find the node with startId or the closest one
    let current = this.header;
    let startNode: SkipNode<T> | null = null;

    // Search for the startId using skip list levels
    for (let i = this.level; i >= 0; i--) {
      while (
        current.forward[i] !== this.tail &&
        current.forward[i]!.value.id < startId
      ) {
        current = current.forward[i]!;
      }
    }

    // Get the node at the exact ID or the next one
    const nextNode = current.forward[0]!;

    if (nextNode !== this.tail && nextNode.value.id === startId) {
      // Exact match
      startNode = nextNode;
    } else if (direction === 1) {
      // For forward search, use the next node if available
      if (nextNode !== this.tail) {
        startNode = nextNode;
      }
    } else if (direction === -1) {
      // For backward search, use the current node (which is just before the target id)
      if (current !== this.header) {
        startNode = current;
      }
    }

    // If we found a starting point
    if (
      startNode !== null &&
      startNode !== this.header &&
      startNode !== this.tail
    ) {
      // Collect items in the specified direction
      let collected = 0;
      let node: SkipNode<T> | null = startNode;

      if (direction === 1) {
        // Forward direction - follow forward pointers
        while (node !== null && node !== this.tail && collected < count) {
          result.push(node.value);
          collected++;
          node = node.forward[0];
        }
      } else {
        // Backward direction - follow backward pointers
        while (node !== null && node !== this.header && collected < count) {
          result.push(node.value);
          collected++;
          node = node.backward[0];
        }
      }
    }

    return result;
  }

  /**
   * Get the first node in the skip list
   * @returns The first item or null if empty
   */
  public getFirst(): T | null {
    const firstNode = this.header.forward[0];
    return firstNode !== null && firstNode !== this.tail
      ? firstNode.value
      : null;
  }

  /**
   * Get the last node in the skip list
   * @returns The last item or null if empty
   */
  public getLast(): T | null {
    const lastNode = this.tail.backward[0];
    return lastNode !== null && lastNode !== this.header
      ? lastNode.value
      : null;
  }

  public getTotalCount(): number {
    return this.size;
  }

  /**
   * Get the current maximum level of the skip list
   * @returns The current maximum level
   */
  public getMaxLevel(): number {
    return this.level;
  }

  /**
   * Get all nodes at a specific level of the skip list
   * @param level The level to get nodes from (0 is the base level with all nodes)
   * @returns Array of items at the specified level
   */
  public getNodesAtLevel(level: number): T[] {
    const result: T[] = [];

    // Validate level
    if (level < 0 || level > this.level) {
      return result;
    }

    // Traverse the specified level
    let current = this.header.forward[level];
    while (current !== null && current !== this.tail) {
      result.push(current.value);
      current = current.forward[level];
    }

    return result;
  }
}
