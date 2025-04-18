import { describe, it, expect, vi, beforeEach } from 'vitest';
import memoService from './memo-service'; // Import the instance
import type {
  Memo,
  CreateMemoRequest,
  UpdateMemoRequest,
  GetMemoListFilter,
  MemoSort,
  PagedMemosResponse,
} from './memo-service';
import { MemoSortOrder, MemoSortKey } from './memo-service';
import { getDefaultClient } from '$lib/api/client';

// Declare mock functions first
const mockGet = vi.fn();
const mockPost = vi.fn();
const mockPut = vi.fn();
const mockDelete = vi.fn();

// 模拟 ApiClient
vi.mock('$lib/api/client', () => {
  const mockClient = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  };

  return {
    getDefaultClient: () => mockClient,
  };
});

describe('MemoService', () => {
  let mockApiClient: {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    put: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    // 使用类型断言访问私有属性，仅用于测试
    mockApiClient = vi.mocked(getDefaultClient());

    // 重置所有模拟函数
    vi.resetAllMocks();
  });

  const AUTH_API_PREFIX = '/memo/v1';

  describe('getMemo', () => {
    it('should call apiClient.get with the correct URL and return the memo', async () => {
      const mockMemo: Memo = {
        id: 'memo1',
        userId: 'user1',
        title: 'Test Memo',
        content: 'Test Content',
        createdAt: new Date().toISOString(),
        isCompleted: false,
      };
      mockApiClient.get.mockResolvedValue(mockMemo);

      const memoId = 'memo1';
      const result = await memoService.getMemo(memoId);

      // expect(getDefaultClient).toHaveBeenCalled(); // This might be less reliable depending on mock timing
      expect(mockApiClient.get).toHaveBeenCalledTimes(1);
      expect(mockApiClient.get).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos/${memoId}`,
      );
      expect(result).toEqual(mockMemo);
    });
  });

  describe('createMemo', () => {
    it('should call apiClient.post with the correct URL and request body, and return the new memo', async () => {
      const request: CreateMemoRequest = {
        title: 'New Memo',
        content: 'New Content',
      };
      const mockCreatedMemo: Memo = {
        id: 'memo2',
        userId: 'user1',
        createdAt: new Date().toISOString(),
        isCompleted: false,
        ...request,
      };
      mockApiClient.post.mockResolvedValue(mockCreatedMemo);

      const result = await memoService.createMemo(request);

      expect(mockApiClient.post).toHaveBeenCalledTimes(1);
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos`,
        request,
      );
      expect(result).toEqual(mockCreatedMemo);
    });
  });

  describe('updateMemo', () => {
    it('should call apiClient.put with the correct URL and request body, and return the updated memo', async () => {
      const memoId = 'memo1';
      const request: UpdateMemoRequest = {
        title: 'Updated Memo',
        content: 'Updated Content',
        isCompleted: true,
      };
      const mockUpdatedMemo: Memo = {
        id: memoId,
        userId: 'user1',
        createdAt: new Date().toISOString(),
        ...request,
      };
      mockApiClient.put.mockResolvedValue(mockUpdatedMemo);

      const result = await memoService.updateMemo(memoId, request);

      expect(mockApiClient.put).toHaveBeenCalledTimes(1);
      expect(mockApiClient.put).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos/${memoId}`,
        request,
      );
      expect(result).toEqual(mockUpdatedMemo);
    });
  });

  describe('deleteMemo', () => {
    it('should call apiClient.delete with the correct URL', async () => {
      const memoId = 'memo1';
      mockApiClient.delete.mockResolvedValue(undefined); // delete returns void

      await memoService.deleteMemo(memoId);

      expect(mockApiClient.delete).toHaveBeenCalledTimes(1);
      expect(mockApiClient.delete).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos/${memoId}`,
      );
    });
  });

  describe('getMemos', () => {
    const mockPagedResponse: PagedMemosResponse = {
      items: [],
      nextCursor: undefined,
      hasMore: false,
    };

    it('should call apiClient.get with default parameters if none are provided', async () => {
      mockApiClient.get.mockResolvedValue(mockPagedResponse);
      await memoService.getMemos();

      expect(mockApiClient.get).toHaveBeenCalledTimes(1);
      expect(mockApiClient.get).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos/?limit=10`,
      );
    });

    it('should include limit and cursor in the URL params', async () => {
      mockApiClient.get.mockResolvedValue(mockPagedResponse);
      const limit = 20;
      const cursor = 'nextpagecursor';
      await memoService.getMemos(limit, cursor);

      expect(mockApiClient.get).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos/?limit=${limit}&cursor=${cursor}`,
      );
    });

    it('should include filter parameters in the URL params', async () => {
      mockApiClient.get.mockResolvedValue(mockPagedResponse);
      const filter: GetMemoListFilter = { isCompleted: true };
      await memoService.getMemos(10, undefined, filter);

      expect(mockApiClient.get).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos/?limit=10&filter.isCompleted=true`,
      );

      mockApiClient.get.mockClear(); // Clear for next call
      filter.isCompleted = false;
      await memoService.getMemos(10, undefined, filter);
      expect(mockApiClient.get).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos/?limit=10&filter.isCompleted=false`,
      );
    });

    it('should include sort parameters in the URL params', async () => {
      mockApiClient.get.mockResolvedValue(mockPagedResponse);
      const sort: MemoSort = {
        sortKey: MemoSortKey.CREATED_AT,
        sortOrder: MemoSortOrder.DESC,
      };
      await memoService.getMemos(10, undefined, undefined, sort);

      expect(mockApiClient.get).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos/?limit=10&sort.sortOrder=DESC&sort.sortKey=CREATED_AT`,
      );
    });

    it('should include all parameters (limit, cursor, filter, sort) in the URL params', async () => {
      mockApiClient.get.mockResolvedValue(mockPagedResponse);
      const limit = 5;
      const cursor = 'somecursor';
      const filter: GetMemoListFilter = { isCompleted: false };
      const sort: MemoSort = {
        sortKey: MemoSortKey.CREATED_AT,
        sortOrder: MemoSortOrder.ASC,
      };
      await memoService.getMemos(limit, cursor, filter, sort);

      expect(mockApiClient.get).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/memos/?limit=${limit}&cursor=${cursor}&filter.isCompleted=false&sort.sortOrder=ASC&sort.sortKey=CREATED_AT`,
      );
    });

    it('should return the paged response from apiClient.get', async () => {
      const expectedResponse: PagedMemosResponse = {
        items: [
          {
            id: 'm1',
            userId: 'u1',
            title: 'M1',
            content: 'C1',
            createdAt: 't1',
            isCompleted: false,
          },
          {
            id: 'm2',
            userId: 'u1',
            title: 'M2',
            content: 'C2',
            createdAt: 't2',
            isCompleted: true,
          },
        ],
        nextCursor: 'next',
        hasMore: true,
      };
      mockApiClient.get.mockResolvedValue(expectedResponse);

      const result = await memoService.getMemos(2);

      expect(result).toEqual(expectedResponse);
    });
  });
});
