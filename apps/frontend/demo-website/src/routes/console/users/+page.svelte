<script lang="ts">
  import { onMount, setContext } from 'svelte';
  import {
    createUserManagementContext,
    USER_MANAGEMENT_KEY,
  } from './_contexts/userManagement';
  import UserList from './_components/UserList';
  import UserEditModal from './_components/UserEditModal';
  import UserDeleteModal from './_components/UserDeleteModal';

  // 创建并设置上下文
  const userManagementContext = createUserManagementContext();
  setContext(USER_MANAGEMENT_KEY, userManagementContext);

  // 获取错误状态
  const { error } = userManagementContext;

  // 初始加载用户数据
  onMount(() => {
    userManagementContext.loadUsers(10);
  });
</script>

<div>
  <div class="mb-6 flex items-center justify-between">
    <h1 class="text-2xl font-medium text-gray-800">用户管理</h1>
  </div>

  <!-- 显示全局错误 -->
  {#if $error}
    <div class="mb-4 rounded-md bg-red-50 p-4 text-red-600">
      {$error}
    </div>
  {/if}

  <UserList />
  <UserEditModal />
  <UserDeleteModal />
</div>
