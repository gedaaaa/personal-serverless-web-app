<script lang="ts">
  import { getContext, createEventDispatcher } from 'svelte';
  import { USER_MANAGEMENT_KEY } from '../../../../_contexts/userManagement';
  import type { UserManagementContext } from '../../../../_contexts/userManagement';
  import type { UpdateUserRequest } from '../../../../_services/userService';

  // 事件分发
  const dispatch = createEventDispatcher<{
    save: UpdateUserRequest;
    cancel: void;
  }>();

  // 获取上下文
  const { editingUser } =
    getContext<UserManagementContext>(USER_MANAGEMENT_KEY);

  // 表单状态
  let editEmail = '';
  let editPassword = '';
  let editFullName = '';
  let isAdmin = false;

  // 当编辑用户变化时更新表单
  $: if ($editingUser) {
    editEmail = $editingUser.email;
    editFullName = $editingUser.fullName || '';
    editPassword = '';
    isAdmin = $editingUser.roles.includes('ROLE_ADMIN');
  }

  // 保存编辑
  function handleSave() {
    if (!$editingUser) return;

    const roles = isAdmin ? ['ROLE_USER', 'ROLE_ADMIN'] : ['ROLE_USER'];

    const updateData: UpdateUserRequest = {
      email: editEmail !== $editingUser.email ? editEmail : undefined,
      fullName:
        editFullName !== $editingUser.fullName ? editFullName : undefined,
      roles:
        JSON.stringify(roles) !== JSON.stringify($editingUser.roles)
          ? roles
          : undefined,
    };

    // 只有当密码字段有值时才更新密码
    if (editPassword) {
      updateData.password = editPassword;
    }

    // 如果没有任何更改，直接关闭模态框
    if (Object.keys(updateData).length === 0) {
      dispatch('cancel');
      return;
    }

    dispatch('save', updateData);
  }

  // 取消编辑
  function handleCancel() {
    dispatch('cancel');
  }
</script>

<div class="mt-4 space-y-4">
  <div>
    <label for="username" class="block text-sm font-medium text-gray-700"
      >用户名</label
    >
    <input
      type="text"
      id="username"
      value={$editingUser?.username || ''}
      disabled
      class="mt-1 block w-full rounded-md border border-gray-300 bg-gray-100 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500"
    />
  </div>

  <div>
    <label for="email" class="block text-sm font-medium text-gray-700"
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
    <label for="password" class="block text-sm font-medium text-gray-700"
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
    <label for="fullName" class="block text-sm font-medium text-gray-700"
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
    <label for="isAdmin" class="ml-2 block text-sm text-gray-900"
      >管理员权限</label
    >
  </div>
</div>

<div class="mt-5 sm:mt-6 sm:flex sm:flex-row-reverse">
  <button
    type="button"
    on:click={handleSave}
    class="inline-flex w-full justify-center rounded-md border border-transparent bg-purple-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 sm:ml-3 sm:w-auto sm:text-sm"
  >
    保存
  </button>
  <button
    type="button"
    on:click={handleCancel}
    class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 sm:mt-0 sm:w-auto sm:text-sm"
  >
    取消
  </button>
</div>
