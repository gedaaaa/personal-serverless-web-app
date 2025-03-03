<script lang="ts">
  import { onMount } from 'svelte';
  import { userListStore } from './store';
  import UserListItem from './internal/UserListItem/index.svelte';
  import type { UpdateUserRequest } from '../../_services/userService';

  let loadingItems = $state<Record<string, boolean>>({});

  function setLoading(userId: string, loading: boolean) {
    loadingItems[userId] = loading;
  }

  function cleanupLoadingState(userId: string) {
    delete loadingItems[userId];
  }

  function handleUpdate(userId: string, updateData: UpdateUserRequest) {
    setLoading(userId, true);
    userListStore
      .updateUser(userId, updateData)
      .catch((error) => {
        console.error('Failed to update user:', error);
      })
      .finally(() => {
        setLoading(userId, false);
      });
  }

  function handleDelete(userId: string) {
    setLoading(userId, true);
    userListStore
      .deleteUser(userId)
      .then(() => {
        cleanupLoadingState(userId);
      })
      .catch((error) => {
        console.error('Failed to delete user:', error);
        setLoading(userId, false);
      });
  }

  onMount(() => {
    userListStore.loadUsers(10);
    return () => {
      loadingItems = {};
    };
  });
</script>

<!-- 用户列表UI -->
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
      {#if $userListStore.users.length === 0}
        <tr>
          <td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500"
            >暂无用户数据</td
          >
        </tr>
      {:else}
        {#each $userListStore.users as user (user.id)}
          <UserListItem
            {user}
            isLoading={!!loadingItems[user.id]}
            onUpdate={handleUpdate}
            onDelete={handleDelete}
          />
        {/each}
      {/if}
    </tbody>
  </table>
</div>
