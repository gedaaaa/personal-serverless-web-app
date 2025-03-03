import { writable, derived } from 'svelte/store';
import type { User, UpdateUserRequest } from '../../_services/userService';
import { userService } from '../../_services/userService';

function createUserListStore() {
  const users = writable<User[]>([]);
  const error = writable<string | null>(null);

  async function loadUsers(limit: number, cursor?: string) {
    error.set(null);
    try {
      const response = await userService.getUsers(limit, cursor);
      if (cursor) {
        users.update((current) => [...current, ...response.users]);
      } else {
        users.set(response.users);
      }
    } catch (err) {
      error.set('加载用户列表失败');
      throw err;
    }
  }

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

  async function deleteUser(userId: string) {
    error.set(null);
    try {
      await userService.deleteUser(userId);
      users.update((current) => current.filter((user) => user.id !== userId));
    } catch (err) {
      error.set('删除用户失败');
      throw err;
    }
  }

  const { subscribe } = derived([users, error], ([$users, $error]) => ({
    users: $users,
    error: $error,
  }));

  return {
    subscribe,
    loadUsers,
    updateUser,
    deleteUser,
  };
}

export const userListStore = createUserListStore();
