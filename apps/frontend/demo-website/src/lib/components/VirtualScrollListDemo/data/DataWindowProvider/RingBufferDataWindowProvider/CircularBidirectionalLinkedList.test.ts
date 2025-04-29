import { describe, it, expect } from 'vitest';
import {
  CircularBidirectionalLinkedList,
  CircularNode,
} from './CircularBidirectionalLinkedList';

describe('CircularBidirectionalLinkedList', () => {
  // Test initialization
  it('should initialize an empty linked list', () => {
    const list = new CircularBidirectionalLinkedList();
    expect(list.isEmpty()).toBe(true);
    expect(list.getSize()).toBe(0);
  });

  // Test insertAfterHead method
  it('should insert items after head', () => {
    const list = new CircularBidirectionalLinkedList();

    // Insert first item
    list.insertAfterHead('first');
    expect(list.isEmpty()).toBe(false);
    expect(list.getSize()).toBe(1);

    // Insert second item
    list.insertAfterHead('second');
    expect(list.getSize()).toBe(2);

    // Check values
    const values = list.getValues(2);
    expect(values).toContain('first');
    expect(values).toContain('second');
  });

  // Test removeAfterHead method
  it('should remove items after head', () => {
    const list = new CircularBidirectionalLinkedList();

    // Setup test data
    list.insertAfterHead('first');
    list.insertAfterHead('second');
    expect(list.getSize()).toBe(2);

    // Remove item
    const removedItem = list.removeAfterHead();
    expect(list.getSize()).toBe(1);
    expect(removedItem).toBe('second');

    // Remove another item
    const secondRemoved = list.removeAfterHead();
    expect(list.getSize()).toBe(0);
    expect(secondRemoved).toBe('first');

    // Try to remove from empty list
    const emptyRemoved = list.removeAfterHead();
    expect(emptyRemoved).toBe(null);
  });

  // Test getValues method
  it('should get values from the list', () => {
    const list = new CircularBidirectionalLinkedList();

    // Empty list should return empty array
    expect(list.getValues(5)).toEqual([]);

    // Add some items
    list.insertAfterHead('third');
    list.insertAfterHead('second');
    list.insertAfterHead('first');

    // Get all values
    const values = list.getValues(3);
    expect(values.length).toBe(3);
    expect(values[0]).toBe('first');
    expect(values[1]).toBe('second');
    expect(values[2]).toBe('third');

    // Get partial values
    const partialValues = list.getValues(2);
    expect(partialValues.length).toBe(2);
    expect(partialValues[0]).toBe('first');
    expect(partialValues[1]).toBe('second');
  });

  // Test ensureSize method
  it('should ensure the list has a minimum size', () => {
    const list = new CircularBidirectionalLinkedList();

    // Ensure size on empty list
    list.ensureSize(3);
    expect(list.getSize()).toBe(3);

    // Values should be null
    const values = list.getValues(3);
    expect(values).toEqual([null, null, null]);

    // Ensure smaller size (should not change)
    list.ensureSize(2);
    expect(list.getSize()).toBe(3);

    // Ensure larger size
    list.ensureSize(5);
    expect(list.getSize()).toBe(5);
  });

  // Test reset method
  it('should reset the list to a specific size', () => {
    const list = new CircularBidirectionalLinkedList();

    // Add some items
    list.insertAfterHead('first');
    list.insertAfterHead('second');
    expect(list.getSize()).toBe(2);

    // Reset to larger size
    list.reset(4);
    expect(list.getSize()).toBe(4);

    // All values should be null
    const values = list.getValues(4);
    expect(values).toEqual([null, null, null, null]);

    // Reset to smaller size
    list.reset(1);
    expect(list.getSize()).toBe(1);
    expect(list.getValues(1)).toEqual([null]);
  });

  // Test toArray method
  it('should convert list to array', () => {
    const list = new CircularBidirectionalLinkedList();

    // Empty list
    expect(list.toArray()).toEqual([]);

    // List with values
    list.insertAfterHead('third');
    list.insertAfterHead('second');
    list.insertAfterHead('first');

    expect(list.toArray()).toEqual(['first', 'second', 'third']);
  });

  // Test getHead method
  it('should get the head node', () => {
    const list = new CircularBidirectionalLinkedList();
    const head = list.getHead();

    expect(head).toBeInstanceOf(CircularNode);
    expect(head.value).toBe(null); // Head is a sentinel node
  });

  // Test CircularNode methods
  it('should work with CircularNode operations', () => {
    const list = new CircularBidirectionalLinkedList();
    const head = list.getHead();

    // Insert nodes after head
    const nodeA = head.insertAfter('A');
    const nodeB = nodeA.insertAfter('B');
    const nodeC = nodeB.insertAfter('C');

    // Check node values
    expect(nodeA.value).toBe('A');
    expect(nodeB.value).toBe('B');
    expect(nodeC.value).toBe('C');

    // Check links
    expect(head.getNext()).toBe(nodeA);
    expect(nodeA.getNext()).toBe(nodeB);
    expect(nodeB.getNext()).toBe(nodeC);
    expect(nodeC.getNext()).toBe(head);

    expect(head.getPrev()).toBe(nodeC);
    expect(nodeA.getPrev()).toBe(head);
    expect(nodeB.getPrev()).toBe(nodeA);
    expect(nodeC.getPrev()).toBe(nodeB);

    // Remove a node
    const nextAfterRemove = nodeB.remove();
    expect(nextAfterRemove).toBe(nodeC);

    // Check updated links
    expect(nodeA.getNext()).toBe(nodeC);
    expect(nodeC.getPrev()).toBe(nodeA);
  });
});
