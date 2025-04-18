import { describe, it, expect, vi, beforeEach } from 'vitest';
import * as memoStore from './memoStore.svelte';
import memoService from '../_services/memo-service';
import type { Memo, GetMemoListFilter } from '../_services/memo-service';

// Mock memoService
vi.mock('../_services/memo-service', () => ({
  default: {
    getMemos: vi.fn(),
    updateMemo: vi.fn(),
    deleteMemo: vi.fn(),
  },
}));

describe('memoStore', () => {
  const mockMemos: Memo[] = [
    {
      id: '1',
      userId: 'user1',
      title: 'Test Memo 1',
      content: 'Content 1',
      createdAt: '2023-01-01T00:00:00Z',
      isCompleted: false,
    },
    {
      id: '2',
      userId: 'user1',
      title: 'Test Memo 2',
      content: 'Content 2',
      createdAt: '2023-01-02T00:00:00Z',
      isCompleted: true,
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();

    // Reset store to initial state
    memoStore.store.memos = [];
    memoStore.store.error = undefined;
    memoStore.store.filter = { isCompleted: false };
    memoStore.store.isFetchingList = false;
    memoStore.store.hasMore = true;
    memoStore.store.listCursor = undefined;
    memoStore.store.itemLoadingStatus = {};
  });

  describe('fetchMemos', () => {
    it('should fetch memos successfully', async () => {
      // Setup mock
      const mockResponse = {
        items: mockMemos,
        nextCursor: 'next-cursor',
        hasMore: true,
      };
      vi.mocked(memoService.getMemos).mockResolvedValue(mockResponse);

      // Fetch memos
      await memoStore.fetchMemos(10);

      // Verify store state
      expect(memoStore.store.memos).toEqual(mockMemos);
      expect(memoStore.store.listCursor).toBe('next-cursor');
      expect(memoStore.store.hasMore).toBe(true);
      expect(memoStore.store.isFetchingList).toBe(false);
      expect(memoStore.store.error).toBeUndefined();
      expect(memoService.getMemos).toHaveBeenCalledWith(10, undefined, {
        isCompleted: false,
      });
    });

    it('should append memos when cursor is provided', async () => {
      // Setup initial state with existing memos
      memoStore.store.memos = [mockMemos[0]];
      memoStore.store.listCursor = 'some-cursor';

      // Setup mock response for next page
      const mockResponse = {
        items: [mockMemos[1]],
        nextCursor: 'next-cursor',
        hasMore: false,
      };
      vi.mocked(memoService.getMemos).mockResolvedValue(mockResponse);

      // Fetch more memos
      await memoStore.fetchMemos(10);

      // Verify store state
      expect(memoStore.store.memos).toEqual(mockMemos);
      expect(memoStore.store.listCursor).toBe('next-cursor');
      expect(memoStore.store.hasMore).toBe(false);
      expect(memoService.getMemos).toHaveBeenCalledWith(10, 'some-cursor', {
        isCompleted: false,
      });
    });

    it('should handle error when fetching memos', async () => {
      // Setup mock to throw error
      vi.mocked(memoService.getMemos).mockRejectedValue(
        new Error('Failed to load memos'),
      );

      // Fetch memos
      await memoStore.fetchMemos(10);

      // Verify store state
      expect(memoStore.store.memos).toEqual([]);
      expect(memoStore.store.error).toBe(
        'Failed to load memos. Please try again.',
      );
      expect(memoStore.store.isFetchingList).toBe(false);
    });

    it('should not fetch if already fetching', async () => {
      // Set fetching state to true
      memoStore.store.isFetchingList = true;

      // Try to fetch memos
      await memoStore.fetchMemos(10);

      // Verify service was not called
      expect(memoService.getMemos).not.toHaveBeenCalled();
    });

    it('should discard results if filter changed during fetch', async () => {
      // Setup mock with delayed response
      vi.mocked(memoService.getMemos).mockImplementation(async () => {
        // Change filter during fetch
        memoStore.store.filter = { isCompleted: true };

        return {
          items: mockMemos,
          nextCursor: 'next-cursor',
          hasMore: true,
        };
      });

      // Fetch memos
      await memoStore.fetchMemos(10);

      // Verify store state did not update with fetched memos
      expect(memoStore.store.memos).toEqual([]);
    });
  });

  describe('applyFilter', () => {
    it('should apply filter and refresh memos', async () => {
      // Setup mock
      const mockResponse = {
        items: [mockMemos[1]],
        nextCursor: 'next-cursor',
        hasMore: true,
      };
      vi.mocked(memoService.getMemos).mockResolvedValue(mockResponse);

      // Apply filter
      const newFilter: GetMemoListFilter = { isCompleted: true };
      memoStore.applyFilter(newFilter);

      // Verify store state
      expect(memoStore.store.filter).toEqual(newFilter);
      expect(memoStore.store.memos).toEqual([]);
      expect(memoStore.store.listCursor).toBeUndefined();
      expect(memoStore.store.hasMore).toBe(true);

      // Verify getMemos was called with new filter
      expect(memoService.getMemos).toHaveBeenCalledWith(
        10,
        undefined,
        newFilter,
      );
    });
  });

  describe('updateMemoInList', () => {
    it('should update memo in list', () => {
      // Setup initial state
      memoStore.store.memos = [...mockMemos];
      memoStore.store.filter = { isCompleted: true };

      // Update memo
      const updatedData = {
        title: 'Updated Title',
        content: 'Updated Content',
        isCompleted: true,
      };
      memoStore.updateMemoInList('1', updatedData);

      // Verify store state (only first memo should be updated and present due to filter)
      expect(memoStore.store.memos[0].title).toBe('Updated Title');
      expect(memoStore.store.memos[0].content).toBe('Updated Content');
      expect(memoStore.store.memos[1]).toEqual(mockMemos[1]); // Second memo unchanged
    });

    it('should filter out memos that do not match filter after update', () => {
      // Setup initial state
      memoStore.store.memos = [...mockMemos];
      memoStore.store.filter = { isCompleted: false };

      // Update memo to completed
      memoStore.updateMemoInList('1', { isCompleted: true });

      // Verify memo was filtered out (since filter is for incomplete memos)
      expect(memoStore.store.memos.length).toBe(0);
    });
  });

  describe('removeMemoFromList', () => {
    it('should remove memo from list', () => {
      // Setup initial state
      memoStore.store.memos = [...mockMemos];

      // Remove memo
      memoStore.removeMemoFromList('1');

      // Verify store state
      expect(memoStore.store.memos.length).toBe(1);
      expect(memoStore.store.memos[0].id).toBe('2');
    });
  });

  describe('setError', () => {
    it('should set error message', () => {
      // Set error
      memoStore.setError('Test error message');

      // Verify store state
      expect(memoStore.store.error).toBe('Test error message');
    });

    it('should clear error message', () => {
      // Set error first
      memoStore.store.error = 'Existing error';

      // Clear error
      memoStore.setError(undefined);

      // Verify store state
      expect(memoStore.store.error).toBeUndefined();
    });
  });

  describe('setItemLoadingStatus', () => {
    it('should set loading status for specific item', () => {
      // Set loading status
      memoStore.setItemLoadingStatus('1', true);

      // Verify store state
      expect(memoStore.store.itemLoadingStatus['1']).toBe(true);
    });

    it('should update existing loading status', () => {
      // Setup initial state
      memoStore.store.itemLoadingStatus = { '1': true, '2': true };

      // Update loading status
      memoStore.setItemLoadingStatus('1', false);

      // Verify store state
      expect(memoStore.store.itemLoadingStatus['1']).toBe(false);
      expect(memoStore.store.itemLoadingStatus['2']).toBe(true);
    });
  });

  describe('tryFetchNextMemo', () => {
    it('should not fetch if cursor is already set', () => {
      // Setup initial state with cursor
      memoStore.store.listCursor = 'some-cursor';

      // Try to fetch next memo
      memoStore.tryFetchNextMemo();

      // Verify getMemos was not called
      expect(memoService.getMemos).not.toHaveBeenCalled();
    });

    it('should fetch next memo if no cursor and memos exist', async () => {
      // Setup initial state
      memoStore.store.memos = [...mockMemos];
      memoStore.store.listCursor = undefined;

      // Setup mock
      const mockResponse = {
        items: [],
        nextCursor: undefined,
        hasMore: false,
      };
      vi.mocked(memoService.getMemos).mockResolvedValue(mockResponse);

      // Try to fetch next memo
      memoStore.tryFetchNextMemo();

      // Verify cursor was set to last memo id
      expect(memoStore.store.listCursor).toBe('2');
      // Verify getMemos was called with limit 1
      expect(memoService.getMemos).toHaveBeenCalledWith(1, '2', {
        isCompleted: false,
      });
    });
  });
});
