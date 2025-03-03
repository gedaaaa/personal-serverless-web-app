<script lang="ts">
  import { getContext } from 'svelte';
  import { USER_MANAGEMENT_KEY } from '../../_contexts/userManagement';
  import type { UserManagementContext } from '../../_contexts/userManagement';
  import UserListItem from './internal/UserListItem/index.svelte';

  // 获取上下文
  const { users, nextCursor, hasMore, loading, loadMore } =
    getContext<UserManagementContext>(USER_MANAGEMENT_KEY);
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
      {#if $loading && $users.length === 0}
        <tr>
          <td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500"
            >加载中...</td
          >
        </tr>
      {:else if $users.length === 0}
        <tr>
          <td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500"
            >暂无用户数据</td
          >
        </tr>
      {:else}
        {#each $users as user (user.id)}
          <UserListItem {user} />
        {/each}
      {/if}
    </tbody>
  </table>

  {#if $hasMore}
    <div class="flex justify-center border-t border-gray-200 px-4 py-3 sm:px-6">
      <button
        on:click={loadMore}
        disabled={$loading}
        class="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 disabled:opacity-50"
      >
        {$loading ? '加载中...' : '加载更多'}
      </button>
    </div>
  {/if}
</div>
