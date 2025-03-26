import { describe, it, expect } from 'vitest';
import { SkipList } from './SkipList';

describe('SkipList', () => {
  // Test initialization
  it('should initialize an empty SkipList', () => {
    const skipList = new SkipList();
    expect(skipList.getTotalCount()).toBe(0);
  });

  // Test insertion
  it('should insert items correctly', () => {
    const skipList = new SkipList();

    // Insert a single item
    const item = { id: 1, data: { value: 'test' } };
    skipList.insert(item);
    expect(skipList.getTotalCount()).toBe(1);

    // Search for the inserted item
    const foundItem = skipList.search(1);
    expect(foundItem).not.toBeNull();
    expect(foundItem?.id).toBe(1);
    expect(foundItem?.data).toEqual({ value: 'test' });
  });

  // Test removal
  it('should remove items correctly', () => {
    const skipList = new SkipList();

    // Insert and then remove
    skipList.insert({ id: 1, data: { value: 'test1' } });
    skipList.insert({ id: 2, data: { value: 'test2' } });

    const removed = skipList.remove(1);
    expect(removed).not.toBeNull();
    expect(removed?.id).toBe(1);
    expect(skipList.getTotalCount()).toBe(1);

    // Searching for removed item should return null
    const foundItem = skipList.search(1);
    expect(foundItem).toBeNull();
  });

  // Test searching for non-existent items
  it('should return null when searching for non-existent items', () => {
    const skipList = new SkipList();
    const result = skipList.search(999);
    expect(result).toBeNull();
  });

  // Test getRangeFromId functionality
  it('should get a range of items correctly', () => {
    const skipList = new SkipList();

    // Insert items with sequential IDs
    for (let i = 1; i <= 10; i++) {
      skipList.insert({ id: i, data: { value: `test${i}` } });
    }

    // Get range in forward direction
    const forwardRange = skipList.getRangeFromId(3, 1, 5);
    expect(forwardRange.length).toBe(5);
    expect(forwardRange[0].id).toBe(3);
    expect(forwardRange[4].id).toBe(7);

    // Get range in backward direction
    const backwardRange = skipList.getRangeFromId(8, -1, 3);
    expect(backwardRange.length).toBe(3);
    expect(backwardRange[0].id).toBe(8);
    expect(backwardRange[2].id).toBe(6);
  });

  // Test edge cases
  it('should handle edge cases correctly', () => {
    const skipList = new SkipList();

    // Remove from empty list
    const removedFromEmpty = skipList.remove(1);
    expect(removedFromEmpty).toBeNull();

    // Get range from empty list
    const rangeFromEmpty = skipList.getRangeFromId(1, 1, 5);
    expect(rangeFromEmpty.length).toBe(0);

    // Insert multiple items with the same ID (should replace)
    skipList.insert({ id: 1, data: { value: 'original' } });
    skipList.insert({ id: 1, data: { value: 'replacement' } });
    const item = skipList.search(1);
    expect(item?.data).toEqual({ value: 'replacement' });
    expect(skipList.getTotalCount()).toBe(1); // Count should still be 1
  });
});
