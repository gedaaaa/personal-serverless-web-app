import { describe, it, expect, vi, beforeEach } from 'vitest';
import { userListStore, type UserListStoreState } from '../store';
import { userService } from '../../../_services/userService';
import type {
  User,
  UpdateUserRequest,
  PagedUsersResponse,
} from '../../../_services/userService';

// Mock userService
vi.mock('../../../_services/userService', () => ({
  userService: {
    getUsers: vi.fn(),
    updateUser: vi.fn(),
    deleteUser: vi.fn(),
  },
}));

describe('userListStore', () => {
  const mockUsers: User[] = [
    {
      id: '1',
      username: 'testuser1',
      email: 'test1@example.com',
      fullName: 'Test User 1',
      roles: ['ROLE_USER'],
    },
    {
      id: '2',
      username: 'testuser2',
      email: 'test2@example.com',
      fullName: 'Test User 2',
      roles: ['ROLE_ADMIN'],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('loadUsers', () => {
    it('should load users successfully', async () => {
      // Setup mock
      const mockResponse: PagedUsersResponse = {
        items: mockUsers,
        nextCursor: null,
        hasMore: false,
      };
      vi.mocked(userService.getUsers).mockResolvedValue(mockResponse);

      // Load users
      await userListStore.loadUsers(10);

      // Check store state
      let storeState: UserListStoreState | undefined;
      userListStore.subscribe((state) => {
        storeState = state as UserListStoreState;
      });

      expect(storeState?.users).toEqual(mockUsers);
      expect(storeState?.error).toBeNull();
      expect(userService.getUsers).toHaveBeenCalledWith(10, undefined);
    });

    it('should handle error when loading users', async () => {
      // Setup mock to throw error
      vi.mocked(userService.getUsers).mockRejectedValue(
        new Error('Failed to load users'),
      );

      // Load users and expect error to be thrown
      await expect(userListStore.loadUsers(10)).rejects.toThrow(
        'Failed to load users',
      );

      // Check store state
      let storeState: UserListStoreState | undefined;
      userListStore.subscribe((state) => {
        storeState = state as UserListStoreState;
      });

      expect(storeState?.error).toBe('Failed to load users');
    });
  });

  describe('updateUser', () => {
    it('should update user successfully', async () => {
      // Setup mock
      const updatedUser: User = {
        ...mockUsers[0],
        email: 'newemail@example.com',
      };
      vi.mocked(userService.updateUser).mockResolvedValue(updatedUser);

      // Update user
      const updateData: UpdateUserRequest = {
        email: 'newemail@example.com',
      };
      await userListStore.updateUser('1', updateData);

      // Check store state
      let storeState: UserListStoreState | undefined;
      userListStore.subscribe((state) => {
        storeState = state as UserListStoreState;
      });

      expect(storeState?.users[0]).toEqual(updatedUser);
      expect(storeState?.error).toBeNull();
      expect(userService.updateUser).toHaveBeenCalledWith('1', updateData);
    });

    it('should handle error when updating user', async () => {
      // Setup mock to throw error
      vi.mocked(userService.updateUser).mockRejectedValue(
        new Error('Failed to update user'),
      );

      // Update user and expect error to be thrown
      const updateData: UpdateUserRequest = {
        email: 'newemail@example.com',
      };
      await expect(userListStore.updateUser('1', updateData)).rejects.toThrow(
        'Failed to update user',
      );

      // Check store state
      let storeState: UserListStoreState | undefined;
      userListStore.subscribe((state) => {
        storeState = state as UserListStoreState;
      });

      expect(storeState?.error).toBe('更新用户失败');
    });
  });

  describe('deleteUser', () => {
    it('should delete user successfully', async () => {
      // Setup mock
      vi.mocked(userService.deleteUser).mockResolvedValue(undefined);

      // Delete user
      await userListStore.deleteUser('1');

      // Check store state
      let storeState: UserListStoreState | undefined;
      userListStore.subscribe((state) => {
        storeState = state as UserListStoreState;
      });

      expect(storeState?.users).toHaveLength(1);
      expect(storeState?.users[0].id).toBe('2');
      expect(storeState?.error).toBeNull();
      expect(userService.deleteUser).toHaveBeenCalledWith('1');
    });

    it('should handle error when deleting user', async () => {
      // Setup mock to throw error
      vi.mocked(userService.deleteUser).mockRejectedValue(
        new Error('Failed to delete user'),
      );

      // Delete user and expect error to be thrown
      await expect(userListStore.deleteUser('1')).rejects.toThrow(
        'Failed to delete user',
      );

      // Check store state
      let storeState: UserListStoreState | undefined;
      userListStore.subscribe((state) => {
        storeState = state as UserListStoreState;
      });

      expect(storeState?.error).toBe('Failed to delete user');
    });
  });
});
