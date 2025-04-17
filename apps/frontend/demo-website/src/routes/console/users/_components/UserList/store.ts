import { writable, derived } from 'svelte/store';
import type { User, UpdateUserRequest } from '../../_services/userService';
import { userService } from '../../_services/userService';

export interface UserListStoreState {
  users: User[];
  error: string | null;
}

/**
 * Store for managing user list
 * @returns {subscribe: () => UserListStore, loadUsers: () => Promise<void>, updateUser: () => Promise<User>, deleteUser: () => Promise<void>} User list store
 */
function createUserListStore() {
  const users = writable<User[]>([]);
  const error = writable<string | null>(null);

  /**
   * Load users
   * @param limit - The number of users to load
   * @param cursor - The cursor to load the next page of users
   * @returns {Promise<void>}
   */
  async function loadUsers(limit: number, cursor?: string) {
    error.set(null);
    try {
      const response = await userService.getUsers(limit, cursor);
      if (cursor) {
        users.update((current) => [...current, ...response.items]);
      } else {
        users.set(response.items);
      }
    } catch (err) {
      error.set('Failed to load users');
      throw err;
    }
  }

  /**
   * Update a user
   * @param userId - The ID of the user to update
   * @param updateData - The data to update the user with
   * @returns {Promise<User>} The updated user
   */
  async function updateUser(userId: string, updateData: UpdateUserRequest) {
    error.set(null);
    try {
      const updatedUser = await userService.updateUser(userId, updateData);
      users.update((current) =>
        current.map((user) => (user.id === userId ? updatedUser : user)),
      );
      return updatedUser;
    } catch (err) {
      error.set('更新用户失败');
      throw err;
    }
  }

  /**
   * Delete a user
   * @param userId - The ID of the user to delete
   */
  async function deleteUser(userId: string) {
    error.set(null);
    try {
      await userService.deleteUser(userId);
      users.update((current) => current.filter((user) => user.id !== userId));
    } catch (err) {
      error.set('Failed to delete user');
      throw err;
    }
  }

  /**
   * Subscribe to the user list store
   * @returns {Object} User list store
   */
  const { subscribe } = derived(
    [users, error],
    ([$users, $error]) =>
      ({
        users: $users,
        error: $error,
      }) as UserListStoreState,
  );

  return {
    subscribe,
    loadUsers,
    updateUser,
    deleteUser,
  };
}

export const userListStore = createUserListStore();
