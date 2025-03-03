import { writable, type Writable, get } from 'svelte/store';
import { userService } from '../_services/userService';
import type { User, UpdateUserRequest } from '../_services/userService';

// 定义上下文类型
export interface UserManagementContext {
  // 状态
  users: Writable<User[]>;
  nextCursor: Writable<string | null>;
  hasMore: Writable<boolean>;
  loading: Writable<boolean>;
  error: Writable<string | null>;
  editingUser: Writable<User | null>;
  showEditModal: Writable<boolean>;
  deletingUser: Writable<User | null>;
  showDeleteModal: Writable<boolean>;

  // 方法
  loadUsers: (limit: number, cursor?: string) => Promise<void>;
  loadMore: () => Promise<void>;
  openEditModal: (user: User) => void;
  closeEditModal: () => void;
  saveUserEdit: (updateData: UpdateUserRequest) => Promise<void>;
  openDeleteModal: (user: User) => void;
  closeDeleteModal: () => void;
  deleteUser: () => Promise<void>;
}

// 创建上下文键
export const USER_MANAGEMENT_KEY = Symbol('userManagement');

// 创建上下文工厂函数
export function createUserManagementContext(): UserManagementContext {
  // 创建状态
  const users = writable<User[]>([]);
  const nextCursor = writable<string | null>(null);
  const hasMore = writable<boolean>(false);
  const loading = writable<boolean>(true);
  const error = writable<string | null>(null);
  const editingUser = writable<User | null>(null);
  const showEditModal = writable<boolean>(false);
  const deletingUser = writable<User | null>(null);
  const showDeleteModal = writable<boolean>(false);

  // 加载用户列表
  async function loadUsers(limit: number, cursor?: string) {
    loading.set(true);
    error.set(null);

    try {
      const response = await userService.getUsers(limit, cursor);

      if (cursor) {
        // 追加数据
        users.update((current) => [...current, ...response.users]);
      } else {
        // 替换数据
        users.set(response.users);
      }

      nextCursor.set(response.nextCursor);
      hasMore.set(response.hasMore);
    } catch (err) {
      console.error('Failed to load users:', err);
      error.set('加载用户列表失败，请重试');
    } finally {
      loading.set(false);
    }
  }

  // 加载更多用户
  async function loadMore() {
    let currentNextCursor: string | null = null;
    nextCursor.subscribe((value) => {
      currentNextCursor = value;
    })();

    let currentHasMore = false;
    hasMore.subscribe((value) => {
      currentHasMore = value;
    })();

    if (currentHasMore && currentNextCursor) {
      await loadUsers(10, currentNextCursor);
    }
  }

  // 打开编辑用户模态框
  function openEditModal(user: User) {
    editingUser.set(user);
    showEditModal.set(true);
  }

  // 关闭编辑用户模态框
  function closeEditModal() {
    showEditModal.set(false);
    setTimeout(() => editingUser.set(null), 300); // 动画结束后清除
  }

  // 保存用户编辑
  async function saveUserEdit(updateData: UpdateUserRequest) {
    try {
      loading.set(true);
      error.set(null);

      const currentEditingUser = get(editingUser);
      if (!currentEditingUser) return;

      const updatedUser = await userService.updateUser(
        currentEditingUser.id,
        updateData,
      );

      // 更新列表中的用户数据
      users.update((current) =>
        current.map((u) => {
          if (u.id === currentEditingUser.id) {
            return {
              ...u,
              email: updateData.email ?? u.email,
              fullName: updateData.fullName ?? u.fullName,
              roles: updateData.roles ?? u.roles,
            };
          }
          return u;
        }),
      );

      closeEditModal();
    } catch (err) {
      console.error('Failed to update user:', err);
      error.set('更新用户失败，请重试');
    } finally {
      loading.set(false);
    }
  }

  // 打开删除用户模态框
  function openDeleteModal(user: User) {
    deletingUser.set(user);
    showDeleteModal.set(true);
  }

  // 关闭删除用户模态框
  function closeDeleteModal() {
    showDeleteModal.set(false);
    setTimeout(() => deletingUser.set(null), 300); // 动画结束后清除
  }

  // 删除用户
  async function deleteUser() {
    try {
      loading.set(true);
      error.set(null);

      const currentDeletingUser = get(deletingUser);
      if (!currentDeletingUser) return;

      await userService.deleteUser(currentDeletingUser.id);

      // 从列表中移除用户
      users.update((current) =>
        current.filter((u) => u.id !== currentDeletingUser.id),
      );

      closeDeleteModal();
    } catch (err) {
      console.error('Failed to delete user:', err);
      error.set('删除用户失败，请重试');
    } finally {
      loading.set(false);
    }
  }

  return {
    // 状态
    users,
    nextCursor,
    hasMore,
    loading,
    error,
    editingUser,
    showEditModal,
    deletingUser,
    showDeleteModal,

    // 方法
    loadUsers,
    loadMore,
    openEditModal,
    closeEditModal,
    saveUserEdit,
    openDeleteModal,
    closeDeleteModal,
    deleteUser,
  };
}
