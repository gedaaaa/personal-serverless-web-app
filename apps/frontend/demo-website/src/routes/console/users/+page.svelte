<script lang="ts">
  import { onMount } from 'svelte';
  import {
    UserService,
    type User,
    type UpdateUserRequest,
  } from '$lib/services/user-service';

  // 用户列表状态
  let users: User[] = [];
  let nextCursor: string | null = null;
  let hasMore = false;
  let loading = true;
  let error: string | null = null;

  // 分页参数
  let limit = 10;

  // 编辑用户状态
  let editingUser: User | null = null;
  let editEmail = '';
  let editPassword = '';
  let editFullName = '';
  let isAdmin = false;
  let showEditModal = false;

  // 删除用户状态
  let deletingUser: User | null = null;
  let showDeleteModal = false;

  const userService = new UserService();

  onMount(async () => {
    await loadUsers();
  });

  // 加载用户列表
  async function loadUsers(cursor?: string) {
    loading = true;
    error = null;

    try {
      const response = await userService.getUsers(limit, cursor);
      users = cursor ? [...users, ...response.users] : response.users;
      nextCursor = response.nextCursor;
      hasMore = response.hasMore;
    } catch (err) {
      console.error('Failed to load users:', err);
      error = '加载用户列表失败，请重试';
    } finally {
      loading = false;
    }
  }

  // 加载更多用户
  async function loadMore() {
    if (hasMore && nextCursor) {
      await loadUsers(nextCursor);
    }
  }

  // 打开编辑用户模态框
  function openEditModal(user: User) {
    editingUser = user;
    editEmail = user.email;
    editFullName = user.fullName || '';
    editPassword = '';
    isAdmin = user.roles.includes('ROLE_ADMIN');
    showEditModal = true;
  }

  // 关闭编辑用户模态框
  function closeEditModal() {
    showEditModal = false;
    editingUser = null;
  }

  // 保存用户编辑
  async function saveUserEdit() {
    if (!editingUser) return;

    try {
      const roles = isAdmin ? ['ROLE_USER', 'ROLE_ADMIN'] : ['ROLE_USER'];

      const updateData: UpdateUserRequest = {
        email: editEmail !== editingUser.email ? editEmail : undefined,
        fullName:
          editFullName !== editingUser.fullName ? editFullName : undefined,
        roles:
          JSON.stringify(roles) !== JSON.stringify(editingUser.roles)
            ? roles
            : undefined,
      };

      // 只有当密码字段有值时才更新密码
      if (editPassword) {
        updateData.password = editPassword;
      }

      // 如果没有任何更改，直接关闭模态框
      if (Object.keys(updateData).length === 0) {
        closeEditModal();
        return;
      }

      await userService.updateUser(editingUser.id, updateData);

      // 更新本地用户列表
      users = users.map((u) => {
        if (u.id === editingUser?.id) {
          return {
            ...u,
            email: editEmail,
            fullName: editFullName,
            roles: roles,
          };
        }
        return u;
      });

      closeEditModal();
    } catch (err) {
      console.error('Failed to update user:', err);
      error = '更新用户失败，请重试';
    }
  }

  // 打开删除用户模态框
  function openDeleteModal(user: User) {
    deletingUser = user;
    showDeleteModal = true;
  }

  // 关闭删除用户模态框
  function closeDeleteModal() {
    showDeleteModal = false;
    deletingUser = null;
  }

  // 删除用户
  async function deleteUser() {
    if (!deletingUser) return;

    try {
      await userService.deleteUser(deletingUser.id);

      // 从本地用户列表中移除
      users = users.filter((u) => u.id !== deletingUser?.id);

      closeDeleteModal();
    } catch (err) {
      console.error('Failed to delete user:', err);
      error = '删除用户失败，请重试';
    }
  }
</script>

<div>
  <div class="mb-6 flex items-center justify-between">
    <h1 class="text-2xl font-medium text-gray-800">用户管理</h1>
  </div>

  {#if error}
    <div class="mb-4 rounded-md bg-red-50 p-4 text-red-600">{error}</div>
  {/if}

  <!-- 用户列表 -->
  <div class="overflow-x-auto rounded-lg bg-white shadow">
    <table class="min-w-full divide-y divide-gray-200">
      <thead class="bg-gray-50">
        <tr>
          <th
            scope="col"
            class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
            >用户名</th
          >
          <th
            scope="col"
            class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
            >邮箱</th
          >
          <th
            scope="col"
            class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
            >姓名</th
          >
          <th
            scope="col"
            class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
            >角色</th
          >
          <th
            scope="col"
            class="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500"
            >操作</th
          >
        </tr>
      </thead>
      <tbody class="divide-y divide-gray-200 bg-white">
        {#if loading && users.length === 0}
          <tr>
            <td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500"
              >加载中...</td
            >
          </tr>
        {:else if users.length === 0}
          <tr>
            <td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500"
              >暂无用户数据</td
            >
          </tr>
        {:else}
          {#each users as user (user.id)}
            <tr>
              <td
                class="whitespace-nowrap px-6 py-4 text-sm font-medium text-gray-900"
                >{user.username}</td
              >
              <td class="whitespace-nowrap px-6 py-4 text-sm text-gray-500"
                >{user.email}</td
              >
              <td class="whitespace-nowrap px-6 py-4 text-sm text-gray-500"
                >{user.fullName || '-'}</td
              >
              <td class="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                {#if user.roles.includes('ROLE_ADMIN')}
                  <span
                    class="inline-flex rounded-full bg-purple-100 px-2 text-xs font-semibold leading-5 text-purple-800"
                    >管理员</span
                  >
                {:else}
                  <span
                    class="inline-flex rounded-full bg-gray-100 px-2 text-xs font-semibold leading-5 text-gray-800"
                    >普通用户</span
                  >
                {/if}
              </td>
              <td
                class="whitespace-nowrap px-6 py-4 text-right text-sm font-medium"
              >
                <button
                  on:click={() => openEditModal(user)}
                  class="mr-2 text-purple-600 hover:text-purple-900"
                >
                  编辑
                </button>
                <button
                  on:click={() => openDeleteModal(user)}
                  class="text-red-600 hover:text-red-900"
                >
                  删除
                </button>
              </td>
            </tr>
          {/each}
        {/if}
      </tbody>
    </table>

    {#if hasMore}
      <div
        class="flex justify-center border-t border-gray-200 px-4 py-3 sm:px-6"
      >
        <button
          on:click={loadMore}
          disabled={loading}
          class="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 disabled:opacity-50"
        >
          {loading ? '加载中...' : '加载更多'}
        </button>
      </div>
    {/if}
  </div>

  <!-- 编辑用户模态框 -->
  {#if showEditModal}
    <div class="fixed inset-0 z-10 overflow-y-auto">
      <div
        class="flex min-h-screen items-end justify-center px-4 pb-20 pt-4 text-center sm:block sm:p-0"
      >
        <div class="fixed inset-0 transition-opacity" aria-hidden="true">
          <div class="absolute inset-0 bg-gray-500 opacity-75"></div>
        </div>

        <div
          class="inline-block transform overflow-hidden rounded-lg bg-white text-left align-bottom shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:align-middle"
        >
          <div class="bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
            <div class="sm:flex sm:items-start">
              <div class="mt-3 w-full text-center sm:mt-0 sm:text-left">
                <h3 class="text-lg font-medium leading-6 text-gray-900">
                  编辑用户
                </h3>
                <div class="mt-4 space-y-4">
                  <div>
                    <label
                      for="username"
                      class="block text-sm font-medium text-gray-700"
                      >用户名</label
                    >
                    <input
                      type="text"
                      id="username"
                      value={editingUser?.username || ''}
                      disabled
                      class="mt-1 block w-full rounded-md border border-gray-300 bg-gray-100 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500"
                    />
                  </div>

                  <div>
                    <label
                      for="email"
                      class="block text-sm font-medium text-gray-700"
                      >邮箱</label
                    >
                    <input
                      type="email"
                      id="email"
                      bind:value={editEmail}
                      required
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500"
                    />
                  </div>

                  <div>
                    <label
                      for="password"
                      class="block text-sm font-medium text-gray-700"
                      >密码 (留空表示不修改)</label
                    >
                    <input
                      type="password"
                      id="password"
                      bind:value={editPassword}
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500"
                    />
                    <p class="mt-1 text-xs text-gray-500">
                      密码必须至少8个字符，包含至少一个字母和一个数字
                    </p>
                  </div>

                  <div>
                    <label
                      for="fullName"
                      class="block text-sm font-medium text-gray-700"
                      >姓名</label
                    >
                    <input
                      type="text"
                      id="fullName"
                      bind:value={editFullName}
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500"
                    />
                  </div>

                  <div class="flex items-center">
                    <input
                      type="checkbox"
                      id="isAdmin"
                      bind:checked={isAdmin}
                      class="h-4 w-4 rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                    />
                    <label
                      for="isAdmin"
                      class="ml-2 block text-sm text-gray-900">管理员权限</label
                    >
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6">
            <button
              type="button"
              on:click={saveUserEdit}
              class="inline-flex w-full justify-center rounded-md border border-transparent bg-purple-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 sm:ml-3 sm:w-auto sm:text-sm"
            >
              保存
            </button>
            <button
              type="button"
              on:click={closeEditModal}
              class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 sm:mt-0 sm:w-auto sm:text-sm"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </div>
  {/if}

  <!-- 删除用户确认模态框 -->
  {#if showDeleteModal}
    <div class="fixed inset-0 z-10 overflow-y-auto">
      <div
        class="flex min-h-screen items-end justify-center px-4 pb-20 pt-4 text-center sm:block sm:p-0"
      >
        <div class="fixed inset-0 transition-opacity" aria-hidden="true">
          <div class="absolute inset-0 bg-gray-500 opacity-75"></div>
        </div>

        <div
          class="inline-block transform overflow-hidden rounded-lg bg-white text-left align-bottom shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:align-middle"
        >
          <div class="bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
            <div class="sm:flex sm:items-start">
              <div
                class="mx-auto flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-red-100 sm:mx-0 sm:h-10 sm:w-10"
              >
                <svg
                  class="h-6 w-6 text-red-600"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  aria-hidden="true"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                  />
                </svg>
              </div>
              <div class="mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left">
                <h3 class="text-lg font-medium leading-6 text-gray-900">
                  删除用户
                </h3>
                <div class="mt-2">
                  <p class="text-sm text-gray-500">
                    您确定要删除用户 "{deletingUser?.username}"
                    吗？此操作无法撤销。
                  </p>
                </div>
              </div>
            </div>
          </div>
          <div class="bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6">
            <button
              type="button"
              on:click={deleteUser}
              class="inline-flex w-full justify-center rounded-md border border-transparent bg-red-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 sm:ml-3 sm:w-auto sm:text-sm"
            >
              删除
            </button>
            <button
              type="button"
              on:click={closeDeleteModal}
              class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 sm:mt-0 sm:w-auto sm:text-sm"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </div>
  {/if}
</div>
