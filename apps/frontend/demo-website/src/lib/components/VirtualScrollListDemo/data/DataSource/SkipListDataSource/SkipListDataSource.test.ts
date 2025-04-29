import { describe, it, expect } from 'vitest';
import { SkipListDataSource } from './SkipListDataSource';
import { Direction } from '../DataSource';

describe('SkipListDataSource', () => {
  // Test initialization
  it('should initialize an empty SkipListDataSource', () => {
    const dataSource = new SkipListDataSource();
    expect(dataSource.getTotalCount()).toBe(0);
  });

  // Test insert method
  it('should insert items correctly', () => {
    const dataSource = new SkipListDataSource();

    // Insert a single item
    const item = { id: 1, value: 'test1' };
    const result = dataSource.insert(item);

    expect(result).toBe(true);
    expect(dataSource.getTotalCount()).toBe(2); // Since id + 1

    // Get the item to verify it was inserted
    const items = dataSource.getRangeFromId(1, 1, Direction.FORWARD);
    expect(items.length).toBe(1);
    expect(items[0].id).toBe(1);
    expect(items[0].value).toBe('test1');
  });

  // Test inserting multiple items
  it('should handle multiple item insertions', () => {
    const dataSource = new SkipListDataSource();

    // Insert multiple items
    const items = [
      { id: 1, value: 'test1' },
      { id: 2, value: 'test2' },
      { id: 3, value: 'test3' },
    ];

    items.forEach((item) => dataSource.insert(item));

    // Total count should be highest id + 1
    expect(dataSource.getTotalCount()).toBe(4);

    // Get all items
    const retrievedItems = dataSource.getRangeFromId(1, 3, Direction.FORWARD);
    expect(retrievedItems.length).toBe(3);
    expect(retrievedItems[0].id).toBe(1);
    expect(retrievedItems[1].id).toBe(2);
    expect(retrievedItems[2].id).toBe(3);
  });

  // Test delete method
  it('should delete items correctly', () => {
    const dataSource = new SkipListDataSource();

    // Insert and then delete
    dataSource.insert({ id: 1, value: 'test1' });
    dataSource.insert({ id: 2, value: 'test2' });

    const deleteResult = dataSource.delete(1);
    expect(deleteResult).toBe(true);

    // Item should no longer be retrievable
    const items = dataSource.getRangeFromId(1, 2, Direction.FORWARD);
    expect(items.length).toBe(1);
    expect(items[0].id).toBe(2);
  });

  // Test delete non-existent item
  it('should return false when deleting non-existent items', () => {
    const dataSource = new SkipListDataSource();

    const deleteResult = dataSource.delete(999);
    expect(deleteResult).toBe(false);
  });

  // Test getRangeFromId method - forward direction
  it('should get items in forward direction', () => {
    const dataSource = new SkipListDataSource();

    // Insert items with sequential IDs
    for (let i = 1; i <= 10; i++) {
      dataSource.insert({ id: i, value: `test${i}` });
    }

    // Get range in forward direction
    const items = dataSource.getRangeFromId(3, 3, Direction.FORWARD);

    expect(items.length).toBe(3);
    expect(items[0].id).toBe(3);
    expect(items[1].id).toBe(4);
    expect(items[2].id).toBe(5);
  });

  // Test getRangeFromId method - backward direction
  it('should get items in backward direction', () => {
    const dataSource = new SkipListDataSource();

    // Insert items with sequential IDs
    for (let i = 1; i <= 10; i++) {
      dataSource.insert({ id: i, value: `test${i}` });
    }

    // Get range in backward direction
    const items = dataSource.getRangeFromId(8, 3, Direction.BACKWARD);

    // Results should be ordered from small to large ID
    expect(items.length).toBe(3);
    expect(items[0].id).toBe(6);
    expect(items[1].id).toBe(7);
    expect(items[2].id).toBe(8);
  });

  // Test edge cases
  it('should handle edge cases in getRangeFromId', () => {
    const dataSource = new SkipListDataSource();

    // Empty data source
    let items = dataSource.getRangeFromId(1, 5, Direction.FORWARD);
    expect(items.length).toBe(0);

    // Invalid count
    dataSource.insert({ id: 1, value: 'test1' });
    items = dataSource.getRangeFromId(1, 0, Direction.FORWARD);
    expect(items.length).toBe(0);

    // Invalid startId
    items = dataSource.getRangeFromId(-1, 5, Direction.FORWARD);
    expect(items.length).toBe(0);

    items = dataSource.getRangeFromId(100, 5, Direction.FORWARD);
    expect(items.length).toBe(0);
  });
});
