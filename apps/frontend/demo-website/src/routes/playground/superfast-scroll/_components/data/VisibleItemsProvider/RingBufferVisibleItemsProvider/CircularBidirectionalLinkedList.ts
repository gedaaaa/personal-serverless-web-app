/**
 * Doubly Circular Linked List
 * A basic circular linked list implementation with node-level operations
 */

/**
 * Circular Node class with operations
 */
export class CircularNode<T> {
  /**
   * Create a new circular node
   * @param value Node value
   * @param next Next node (default: self-reference)
   * @param prev Previous node (default: self-reference)
   */
  constructor(
    public value: T | null = null,
    public next: CircularNode<T> = null as unknown as CircularNode<T>,
    public prev: CircularNode<T> = null as unknown as CircularNode<T>,
  ) {
    // Default to self-referencing if not properly initialized
    if (this.next === (null as unknown as CircularNode<T>)) this.next = this;
    if (this.prev === (null as unknown as CircularNode<T>)) this.prev = this;
  }

  /**
   * Insert a new node after this node
   * @param value Value to insert
   * @returns The newly inserted node
   */
  insertAfter(value: T | null): CircularNode<T> {
    const newNode = new CircularNode<T>(value, this.next, this);
    this.next.prev = newNode;
    this.next = newNode;
    return newNode;
  }

  /**
   * Insert a new node before this node
   * @param value Value to insert
   * @returns The newly inserted node
   */
  insertBefore(value: T | null): CircularNode<T> {
    const newNode = new CircularNode<T>(value, this, this.prev);
    this.prev.next = newNode;
    this.prev = newNode;
    return newNode;
  }

  /**
   * Remove this node from the list
   * @returns The next node after removal
   */
  remove(): CircularNode<T> {
    // Connect the previous and next nodes
    this.prev.next = this.next;
    this.next.prev = this.prev;

    // Return the next node
    return this.next;
  }

  /**
   * Get the next node
   * @returns The next node
   */
  getNext(): CircularNode<T> {
    return this.next;
  }

  /**
   * Get the previous node
   * @returns The previous node
   */
  getPrev(): CircularNode<T> {
    return this.prev;
  }

  /**
   * Move forward by n steps
   * @param n Number of steps to move forward
   * @returns The node after moving
   */
  moveForward(n: number): CircularNode<T> {
    let current: CircularNode<T> = this;
    for (let i = 0; i < n; i++) {
      current = current.next;
    }
    return current;
  }

  /**
   * Move backward by n steps
   * @param n Number of steps to move backward
   * @returns The node after moving
   */
  moveBackward(n: number): CircularNode<T> {
    let current: CircularNode<T> = this;
    for (let i = 0; i < n; i++) {
      current = current.prev;
    }
    return current;
  }

  /**
   * Get values of consecutive nodes starting from this node
   * @param count Number of nodes to retrieve
   * @returns Array of values
   */
  getValues(count: number): (T | null)[] {
    const values: (T | null)[] = [];
    let current: CircularNode<T> = this;

    for (let i = 0; i < count; i++) {
      values.push(current.value);
      current = current.next;
      // Break if we've circled back
      if (current === this) break;
    }

    return values;
  }

  /**
   * Set values for consecutive nodes starting from this node
   * @param values Values to set
   * @returns Number of nodes updated
   */
  setValues(values: (T | null)[]): number {
    if (values.length === 0) return 0;

    let current: CircularNode<T> = this;
    let count = 0;

    for (let i = 0; i < values.length; i++) {
      current.value = values[i];
      current = current.next;
      count++;

      // Break if we've circled back
      if (current === this) break;
    }

    return count;
  }

  /**
   * Find a node with the specified value
   * @param value Value to find
   * @param maxNodes Maximum number of nodes to search
   * @returns Node if found, null otherwise
   */
  findNodeWithValue(
    value: T,
    maxNodes: number = Number.MAX_SAFE_INTEGER,
  ): CircularNode<T> | null {
    let current: CircularNode<T> = this;
    let nodesChecked = 0;

    do {
      if (current.value === value) {
        return current;
      }
      current = current.next;
      nodesChecked++;
    } while (current !== this && nodesChecked < maxNodes);

    return null;
  }
}

export class CircularBidirectionalLinkedList<T> {
  // Head node (sentinel node)
  private head: CircularNode<T>;
  // Total size of the list
  private size: number;

  /**
   * Creates a new doubly circular linked list
   * @param initialSize Initial size of the list (filled with null values)
   */
  constructor(initialSize: number = 0) {
    // Create sentinel node
    this.head = new CircularNode<T>(null);
    this.size = 0;

    // Initialize with empty nodes if needed
    if (initialSize > 0) {
      this.ensureSize(initialSize);
    }
  }

  /**
   * Ensure the list has at least minSize nodes
   * @param minSize Minimum size to ensure
   */
  public ensureSize(minSize: number): void {
    const nodesToAdd = Math.max(0, minSize - this.size);
    for (let i = 0; i < nodesToAdd; i++) {
      this.head.insertAfter(null);
      this.size++;
    }
  }

  /**
   * Insert a new node after the head
   * @param value Value to insert
   * @returns The newly inserted node
   */
  public insertAfterHead(value: T | null): CircularNode<T> {
    const newNode = this.head.insertAfter(value);
    this.size++;
    return newNode;
  }

  /**
   * Remove the node after the head
   * @returns The value of the removed node
   */
  public removeAfterHead(): T | null {
    if (this.size <= 0) {
      return null;
    }

    const removedValue = this.head.next.value;
    this.head.next.remove();
    this.size--;

    return removedValue;
  }

  /**
   * Get the head node
   * @returns Head node
   */
  public getHead(): CircularNode<T> {
    return this.head;
  }

  /**
   * Get values starting from the node after head
   * @param count Number of nodes to retrieve
   * @returns Array of values (null values are included)
   */
  public getValues(count: number): (T | null)[] {
    if (this.size === 0) return [];
    return this.head.next.getValues(Math.min(count, this.size));
  }

  /**
   * Set values starting from the node after head
   * @param values Values to set
   * @returns Number of values set
   */
  public setValues(values: (T | null)[]): number {
    if (this.size === 0) return 0;
    return this.head.next.setValues(values);
  }

  /**
   * Get the total size of the list
   * @returns List size
   */
  public getSize(): number {
    return this.size;
  }

  /**
   * Reset the list to the given size, filled with null values
   * @param size New size of the list
   */
  public reset(size: number): void {
    // Remove all nodes except the head
    this.head.next = this.head;
    this.head.prev = this.head;
    this.size = 0;

    // Re-initialize with null values
    this.ensureSize(size);
  }

  /**
   * Find the first node with the given value
   * @param value Value to find
   * @returns Node if found, null otherwise
   */
  public findNode(value: T): CircularNode<T> | null {
    if (this.size === 0) return null;
    return this.head.next.findNodeWithValue(value, this.size);
  }

  /**
   * Check if list is empty
   * @returns True if list is empty
   */
  public isEmpty(): boolean {
    return this.size === 0;
  }

  /**
   * Get node at specific index
   * @param index Index of the node (0-based)
   * @returns Node at index or null if index is out of bounds
   */
  public getNodeAt(index: number): CircularNode<T> | null {
    if (index < 0 || index >= this.size) {
      return null;
    }

    return this.head.next.moveForward(index);
  }

  /**
   * Convert list to array
   * @returns Array containing all values in the list
   */
  public toArray(): (T | null)[] {
    return this.getValues(this.size);
  }
}
