import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { DelayedDataSource } from './DelayedDataSource';
import { Direction } from './DataSource';

// Mock setTimeout to avoid actual delays in tests
vi.useFakeTimers();

describe('DelayedDataSource', () => {
  beforeEach(() => {
    vi.clearAllTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // Test initialization
  it('should initialize with default or custom delay', () => {
    // Default delay
    const defaultDataSource = new DelayedDataSource();
    expect(defaultDataSource).toBeInstanceOf(DelayedDataSource);

    // Custom delay
    const customDataSource = new DelayedDataSource(500);
    expect(customDataSource).toBeInstanceOf(DelayedDataSource);
  });

  // Test insert method (synchronous, passes through to delegate)
  it('should insert items synchronously', () => {
    const dataSource = new DelayedDataSource();

    const item = { id: 1, value: 'test' };
    const result = dataSource.insert(item);

    expect(result).toBe(true);
  });

  // Test delete method (asynchronous, with delay)
  it('should delete items with delay', async () => {
    const dataSource = new DelayedDataSource(100);

    // Setup test data
    dataSource.insert({ id: 1, value: 'test' });

    // Start delete operation
    const deletePromise = dataSource.delete(1);

    // Verify promise is not resolved immediately
    expect(deletePromise).toBeInstanceOf(Promise);

    // Advance timers
    vi.advanceTimersByTime(100);

    // Await the promise to resolve
    const result = await deletePromise;
    expect(result).toBe(true);
  });

  // Test getRangeFromId method (asynchronous, with delay)
  it('should get range of items with delay', async () => {
    const dataSource = new DelayedDataSource(150);

    // Setup test data
    dataSource.insert({ id: 1, value: 'test1' });
    dataSource.insert({ id: 2, value: 'test2' });
    dataSource.insert({ id: 3, value: 'test3' });

    // Start get operation
    const getPromise = dataSource.getRangeFromId(1, 3, Direction.FORWARD);

    // Verify promise is not resolved immediately
    expect(getPromise).toBeInstanceOf(Promise);

    // Advance timers
    vi.advanceTimersByTime(150);

    // Await the promise to resolve
    const items = await getPromise;
    expect(items.length).toBe(3);
    expect(items[0].id).toBe(1);
    expect(items[1].id).toBe(2);
    expect(items[2].id).toBe(3);
  });

  // Test getTotalCount method (asynchronous, with delay)
  it('should get total count with delay', async () => {
    const dataSource = new DelayedDataSource(200);

    // Setup test data
    dataSource.insert({ id: 1, value: 'test1' });
    dataSource.insert({ id: 2, value: 'test2' });

    // Start count operation
    const countPromise = dataSource.getTotalCount();

    // Verify promise is not resolved immediately
    expect(countPromise).toBeInstanceOf(Promise);

    // Advance timers
    vi.advanceTimersByTime(200);

    // Await the promise to resolve
    const count = await countPromise;
    expect(count).toBe(3); // highest id + 1
  });

  // Test delay mechanism with different delays
  it('should respect the delay time specified', async () => {
    // Test with different delay values
    const delays = [50, 100, 200];

    for (const delay of delays) {
      const dataSource = new DelayedDataSource(delay);

      // Setup spy on setTimeout
      const setTimeoutSpy = vi.spyOn(global, 'setTimeout');

      // Trigger an async operation
      const promise = dataSource.getTotalCount();

      // Verify setTimeout was called with the correct delay
      expect(setTimeoutSpy).toHaveBeenLastCalledWith(
        expect.any(Function),
        delay,
      );

      // Complete the operation
      vi.advanceTimersByTime(delay);
      await promise;

      // Clean up spy
      setTimeoutSpy.mockRestore();
    }
  });
});
