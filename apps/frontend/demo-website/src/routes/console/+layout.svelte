<script lang="ts">
  import { auth, logout } from '$lib/auth';
  import { isAdmin } from '$lib/auth/permissions';
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';

  let { children } = $props();

  onMount(() => {
    // 如果用户未登录或不是管理员，重定向到首页
    if (!$auth.isAuthenticated || !isAdmin()) {
      goto('/');
    }
  });

  const handleLogout = () => {
    logout('/');
  };
</script>

<div class="min-h-screen bg-gray-50">
  <!-- 顶部导航栏 -->
  <nav class="bg-white shadow-sm">
    <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
      <div class="flex h-16 justify-between">
        <div class="flex">
          <div class="flex flex-shrink-0 items-center">
            <span class="text-xl font-semibold text-purple-600">管理控制台</span
            >
          </div>
          <div class="ml-6 flex items-center space-x-4">
            <a
              href="/console/users"
              class="px-3 py-2 text-sm font-medium text-gray-700 hover:text-purple-600"
              >用户管理</a
            >
          </div>
        </div>
        <div class="flex items-center">
          {#if $auth.user}
            <span class="mr-4 text-sm text-gray-700"
              >欢迎，{$auth.user.username}</span
            >
          {/if}
          <button
            class="rounded-md bg-gray-200 px-3 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-300"
            onclick={handleLogout}
          >
            退出登录
          </button>
        </div>
      </div>
    </div>
  </nav>

  <!-- 页面内容 -->
  <div class="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
    {@render children()}
  </div>
</div>
