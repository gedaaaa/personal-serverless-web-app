<script lang="ts">
  import { getContext } from 'svelte';
  import { USER_MANAGEMENT_KEY } from '../../../../_contexts/userManagement';
  import type { UserManagementContext } from '../../../../_contexts/userManagement';
  import type { User } from '../../../../_services/userService';

  // 组件属性
  export let user: User;

  // 获取上下文
  const { openEditModal, openDeleteModal } =
    getContext<UserManagementContext>(USER_MANAGEMENT_KEY);
</script>

<tr>
  <td class="whitespace-nowrap px-6 py-4 text-sm font-medium text-gray-900"
    >{user.username}</td
  >
  <td class="whitespace-nowrap px-6 py-4 text-sm text-gray-500">{user.email}</td
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
  <td class="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
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
