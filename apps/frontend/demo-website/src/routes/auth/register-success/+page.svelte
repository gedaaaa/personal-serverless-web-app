<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { AuthService } from '$lib/services/auth-service';

  // 状态定义
  let email = $state<string | null>(null);
  let status = $state('idle'); // idle, sending, success, error
  let message = $state<string | null>(null);
  let lastResendTime = $state<number | null>(null);
  const resendCooldown = 60; // 60 seconds

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
    // 从 localStorage 获取邮箱（通常在注册时保存）
    const storedEmail = localStorage.getItem('pending_verification_email');

    if (storedEmail) {
      email = storedEmail;
    } else {
      // 如果没有找到邮箱，重定向到首页
      goto('/');
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

    return () => clearInterval(timer);
  });

  async function handleResend() {
    if (!canResend || !email) return;

    try {
      status = 'sending';
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
    <h1 class="mb-6 text-center text-2xl font-bold">注册成功</h1>

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

      <h2 class="mb-4 text-xl font-semibold">账号已创建</h2>

      {#if email}
        <p class="mb-2">
          我们已向 <strong>{email}</strong> 发送了一封验证邮件。
        </p>
        <p class="mb-6">请检查您的邮箱并点击验证链接完成注册。</p>
      {:else}
        <p class="mb-6">我们已向您的邮箱发送了一封验证邮件。</p>
      {/if}

      {#if message}
        <div
          class={`mb-4 rounded-md p-3 ${status === 'error' ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-600'}`}
        >
          {message}
        </div>
      {/if}

      <button
        onclick={handleResend}
        disabled={!canResend || status === 'sending' || !email}
        class="mb-4 rounded-md bg-purple-600 px-6 py-2 text-white hover:bg-purple-700 disabled:bg-gray-400"
      >
        {#if status === 'sending'}
          发送中...
        {:else if !canResend}
          重新发送 ({remainingTime}s)
        {:else}
          重新发送验证邮件
        {/if}
      </button>

      <div class="mt-6 border-t border-gray-200 pt-6">
        <a href="/" class="text-purple-600 hover:text-purple-800"> 返回登录 </a>
      </div>
    </div>
  </div>
</div>
