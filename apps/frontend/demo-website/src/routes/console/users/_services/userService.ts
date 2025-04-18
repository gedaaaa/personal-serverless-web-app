import { ApiClient } from '$lib/api';
import { getDefaultClient } from '$lib/api/client';
import type PagedResponse from '$lib/types/PagedResponse';

// Auth API path prefix
const AUTH_API_PREFIX = '/auth/v1';

export interface User {
  id: string;
  username: string;
  email: string;
  roles: string[];
  fullName?: string;
}

export type PagedUsersResponse = PagedResponse<User>;

export interface UpdateUserRequest {
  email?: string;
  password?: string;
  roles?: string[];
  fullName?: string;
}

/**
 * Service for handling user operations
 */
class UserService {
  private apiClient: ApiClient = getDefaultClient();

  /**
   * Get all users with pagination
   * @param limit Maximum number of users to return
   * @param cursor Pagination cursor
   * @returns Paginated list of users
   */
  async getUsers(limit = 10, cursor?: string): Promise<PagedUsersResponse> {
    let endpoint = `${AUTH_API_PREFIX}/users?limit=${limit}`;
    if (cursor) {
      endpoint += `&cursor=${encodeURIComponent(cursor)}`;
    }
    return this.apiClient.get<PagedUsersResponse>(endpoint);
  }

  /**
   * Get user by ID
   * @param id User ID
   * @returns User details
   */
  async getUserById(id: string): Promise<User> {
    return this.apiClient.get<User>(`${AUTH_API_PREFIX}/users/${id}`);
  }

  /**
   * Update user
   * @param id User ID
   * @param data Update data
   */
  async updateUser(id: string, data: UpdateUserRequest): Promise<User> {
    return this.apiClient.put<User>(`${AUTH_API_PREFIX}/users/${id}`, data);
  }

  /**
   * Delete user
   * @param id User ID
   */
  async deleteUser(id: string): Promise<void> {
    await this.apiClient.delete<Record<string, never>>(
      `${AUTH_API_PREFIX}/users/${id}`,
    );
  }
}

// 导出单例实例
export const userService = new UserService();
