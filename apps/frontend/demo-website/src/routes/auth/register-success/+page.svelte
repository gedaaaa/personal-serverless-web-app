<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { AuthService } from '$lib/services/auth-service';

  // State definitions
  let email = $state<string | null>(null);
  let status = $state('idle'); // idle, sending, success, error
  let message = $state<string | null>(null);
  let lastResendTime = $state<number | null>(null);
  const resendCooldown = 60; // 60 seconds

  const authService = new AuthService();

  // Calculate if can resend
  let canResend = $derived(
    !lastResendTime || Date.now() - lastResendTime > resendCooldown * 1000,
  );

  // Remaining time
  let remainingTime = $derived(
    lastResendTime
      ? Math.max(
          0,
          Math.ceil(resendCooldown - (Date.now() - lastResendTime) / 1000),
        )
      : 0,
  );

  onMount(() => {
    // Get email from localStorage (usually saved during registration)
    const storedEmail = localStorage.getItem('pending_verification_email');

    if (storedEmail) {
      email = storedEmail;
    } else {
      // If no email found, redirect to homepage
      goto('/');
    }

    // Start timer to update remaining time
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
      message =
        error instanceof Error
          ? error.message
          : 'Send failed, please try again later';
    }
  }
</script>

<div class="container mx-auto max-w-md px-4 py-12">
  <div class="rounded-lg bg-white p-6 shadow-md">
    <h1 class="mb-6 text-center text-2xl font-bold">Registration Successful</h1>

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

      <h2 class="mb-4 text-xl font-semibold">Account Created</h2>

      {#if email}
        <p class="mb-2">
          We have sent a verification email to <strong>{email}</strong>.
        </p>
        <p class="mb-6">
          Please check your email and click the verification link to complete
          registration.
        </p>
      {:else}
        <p class="mb-6">
          We have sent a verification email to your email address.
        </p>
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
          Sending...
        {:else if !canResend}
          Resend ({remainingTime}s)
        {:else}
          Resend Verification Email
        {/if}
      </button>

      <div class="mt-6 border-t border-gray-200 pt-6">
        <a href="/" class="text-purple-600 hover:text-purple-800">
          Back to Login
        </a>
      </div>
    </div>
  </div>
</div>
