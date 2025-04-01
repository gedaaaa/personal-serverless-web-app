<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { AuthService } from '$lib/services/auth-service';
  import { goto } from '$app/navigation';

  type Status = 'idle' | 'verifying' | 'success' | 'error';

  // 状态定义
  let status = $state<Status>('idle');
  let message = $state<string | null>(null);
  let email = $state<string | null>(null);
  let lastResendTime = $state<number | null>(null);
  const resendCooldown = 60; // 60 seconds

  // 获取验证令牌
  const token = $page.url.searchParams.get('token');
  const authService = new AuthService();

  // 计算是否可以重发
  let canResend = $derived(
    !lastResendTime || Date.now() - lastResendTime > resendCooldown * 1000,
  );

  // 剩余时间
  let remainingTime = $derived(
    lastResendTime
      ? Math.max(
          0,
          Math.ceil(resendCooldown - (Date.now() - lastResendTime) / 1000),
        )
      : 0,
  );

  onMount(() => {
    let cleanup: () => void;

    (async () => {
      if (!token) {
        status = 'error';
        message = '无效的验证链接';
        return;
      }

      try {
        status = 'verifying';
        const result = await authService.verifyEmail(token);
        status = 'success';
        message = result;
      } catch (error: unknown) {
        status = 'error';
        message =
          error instanceof Error ? error.message : '验证失败，请稍后重试';
      }

      // 启动计时器更新剩余时间
      const timer = setInterval(() => {
        if (
          lastResendTime &&
          Date.now() - lastResendTime > resendCooldown * 1000
        ) {
          clearInterval(timer);
        }
      }, 1000);

      cleanup = () => clearInterval(timer);
    })();

    return () => cleanup?.();
  });

  async function handleResend() {
    if (!canResend || !email) return;

    try {
      status = 'verifying';
      const result = await authService.resendVerificationEmail(email);
      status = 'success';
      message = result;
      lastResendTime = Date.now();
    } catch (error) {
      status = 'error';
      message = error instanceof Error ? error.message : '发送失败，请稍后重试';
    }
  }
</script>

<div class="container mx-auto max-w-md px-4 py-12">
  <div class="rounded-lg bg-white p-6 shadow-md">
    <h1 class="mb-6 text-center text-2xl font-bold">邮箱验证</h1>

    {#if status === 'verifying'}
      <div class="flex items-center justify-center py-8">
        <div
          class="h-8 w-8 animate-spin rounded-full border-4 border-purple-500 border-t-transparent"
        ></div>
        <span class="ml-3">正在验证...</span>
      </div>
    {:else if status === 'success'}
      <div class="py-4 text-center">
        <svg
          class="mx-auto mb-4 h-16 w-16 text-green-500"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M5 13l4 4L19 7"
          ></path>
        </svg>
        <h2 class="mb-2 text-xl font-semibold text-green-600">验证成功</h2>
        <p class="mb-6">{message}</p>
        <a
          href="/"
          class="inline-block rounded-md bg-purple-600 px-6 py-2 text-white hover:bg-purple-700"
        >
          前往登录
        </a>
      </div>
    {:else if status === 'error'}
      <div class="py-4 text-center">
        <svg
          class="mx-auto mb-4 h-16 w-16 text-red-500"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M6 18L18 6M6 6l12 12"
          ></path>
        </svg>
        <h2 class="mb-2 text-xl font-semibold text-red-600">验证失败</h2>
        <p class="mb-6">{message}</p>

        <div class="my-6 border-t border-gray-200 pt-6">
          <h3 class="mb-3 font-medium">重新发送验证邮件</h3>
          <div class="mb-4 flex">
            <input
              type="email"
              placeholder="输入您的邮箱"
              bind:value={email}
              class="flex-1 rounded-l-md border border-gray-300 px-3 py-2 focus:outline-none focus:ring-1 focus:ring-purple-500"
            />
            <button
              onclick={handleResend}
              class="rounded-r-md bg-purple-600 px-4 py-2 text-white hover:bg-purple-700 disabled:bg-gray-400"
              disabled={!canResend}
            >
              {canResend ? '重新发送' : `重新发送 (${remainingTime}s)`}
            </button>
          </div>
        </div>

        <a
          href="/"
          class="mt-4 inline-block text-purple-600 hover:text-purple-800"
        >
          返回首页
        </a>
      </div>
    {:else}
      <div class="py-8 text-center">
        <p>正在加载...</p>
      </div>
    {/if}
  </div>
</div>
