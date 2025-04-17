import { ApiClient } from '$lib/api';
import { getDefaultClient } from '$lib/api/client';
import type PagedResponse from '$lib/types/PagedResponse';

const AUTH_API_PREFIX = '/memo/v1';

export interface Memo {
  id: string;
  userId: string;
  title: string;
  content: string;
  createdAt: string;
  reminderTime?: string; // ISO string format for Instant
  isCompleted: boolean;
}

export interface CreateMemoRequest {
  title: string;
  content: string;
  reminderTime?: string; // ISO string format for Instant
}

export interface UpdateMemoRequest {
  title: string;
  content: string;
  reminderTime?: string; // ISO string format for Instant
  isCompleted: boolean;
}

export interface GetMemoListFilter {
  isCompleted?: boolean;

  //   not in use
  //   isDeleted?: boolean;
}

export enum MemoSortOrder {
  ASC = 'ASC',
  DESC = 'DESC',
}

export enum MemoSortKey {
  REMINDER_TIME = 'REMINDER_TIME',
}

export interface MemoSort {
  sortOrder?: MemoSortOrder;
  sortKey?: MemoSortKey;
}

export type PagedMemosResponse = PagedResponse<Memo>;

class MemoService {
  private apiClient: ApiClient = getDefaultClient();

  async getMemo(id: string): Promise<Memo> {
    return this.apiClient.get<Memo>(`${AUTH_API_PREFIX}/memos/${id}`);
  }

  async createMemo(request: CreateMemoRequest): Promise<void> {
    return this.apiClient.post(`${AUTH_API_PREFIX}/memos`, request);
  }

  async updateMemo(id: string, request: UpdateMemoRequest): Promise<Memo> {
    return this.apiClient.put<Memo>(`${AUTH_API_PREFIX}/memos/${id}`, request);
  }

  async deleteMemo(id: string): Promise<void> {
    return this.apiClient.delete(`${AUTH_API_PREFIX}/memos/${id}`);
  }

  async getMemos(
    limit: number = 10,
    cursor?: string,
    filter?: GetMemoListFilter,
    sort?: MemoSort,
  ): Promise<PagedMemosResponse> {
    const params = new URLSearchParams();

    // limit
    params.append('limit', limit.toString());

    // cursor
    if (cursor) {
      params.append('cursor', cursor);
    }

    // filter
    if (filter) {
      if (filter.isCompleted !== undefined) {
        params.append('filter.isCompleted', filter.isCompleted.toString());
      }
    }

    // sort
    if (sort) {
      if (sort.sortOrder) {
        params.append('sort.sortOrder', sort.sortOrder);
      }
      if (sort.sortKey) {
        params.append('sort.sortKey', sort.sortKey);
      }
    }

    return this.apiClient.get<PagedMemosResponse>(
      `${AUTH_API_PREFIX}/memos/?${params.toString()}`,
    );
  }
}

export default new MemoService();
