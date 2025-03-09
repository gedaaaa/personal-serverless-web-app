<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { auth } from '$lib/auth';
  import { AuthService } from '$lib/services/auth-service';

  // Form state
  let isLogin = $state(true);
  let username = $state('');
  let password = $state('');
  let email = $state('');
  let fullName = $state('');
  let error = $state<string | null>(null);
  let loading = $state(false);

  const authService = new AuthService();

  onMount(() => {
    // If user is already logged in, redirect to hello-world
    if ($auth.isAuthenticated) {
      goto('/hello-world');
    }
  });

  const toggleForm = () => {
    isLogin = !isLogin;
    error = null;
  };

  const handleSubmit = async () => {
    error = null;
    loading = true;

    try {
      if (isLogin) {
        // Handle login
        await authService.login({ username, password });
        goto('/hello-world');
      } else {
        // Handle registration
        // Validate email format
        if (!email.includes('@')) {
          error = 'Please enter a valid email address';
          loading = false;
          return;
        }

        // Validate password (at least 8 chars, 1 letter, 1 number)
        const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;
        if (!passwordRegex.test(password)) {
          error =
            'Password must be at least 8 characters long and contain at least one letter and one number';
          loading = false;
          return;
        }

        await authService.register({
          username,
          email,
          password,
          fullName: fullName || undefined,
        });

        // After registration, switch to login form
        isLogin = true;
        error = null;
      }
    } catch (err) {
      console.error('Auth error:', err);
      error = isLogin
        ? 'Login failed. Please check your credentials.'
        : 'Registration failed. Please try again.';
    } finally {
      loading = false;
    }
  };
</script>

<div class="mx-auto my-12 max-w-md px-4">
  <div class="mb-8 text-center">
    <h1 class="text-3xl font-medium text-gray-800">
      {isLogin ? 'Welcome Back' : 'Create Account'}
    </h1>
    <p class="mt-2 text-gray-600">
      {isLogin ? 'Sign in to access your account' : 'Register to get started'}
    </p>
  </div>

  <div class="rounded-lg bg-white p-6 shadow-sm">
    {#if error}
      <div
        class="mb-4 rounded-md bg-red-50 p-4 text-red-600"
        data-testid="error-message"
      >
        {error}
      </div>
    {/if}

    <form on:submit|preventDefault={handleSubmit} class="space-y-4" role="form">
      <div>
        <label for="username" class="block text-sm font-medium text-gray-700"
          >Username</label
        >
        <input
          id="username"
          type="text"
          bind:value={username}
          required
          class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500"
        />
      </div>

      {#if !isLogin}
        <div>
          <label for="email" class="block text-sm font-medium text-gray-700"
            >Email</label
          >
          <input
            id="email"
            type="email"
            bind:value={email}
            required
            class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500"
          />
        </div>

        <div>
          <label for="fullName" class="block text-sm font-medium text-gray-700"
            >Full Name (optional)</label
          >
          <input
            id="fullName"
            type="text"
            bind:value={fullName}
            class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500"
          />
        </div>
      {/if}

      <div>
        <label for="password" class="block text-sm font-medium text-gray-700"
          >Password</label
        >
        <input
          id="password"
          type="password"
          bind:value={password}
          required
          class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500"
        />
        {#if !isLogin}
          <p class="mt-1 text-xs text-gray-500">
            Password must be at least 8 characters long and contain at least one
            letter and one number
          </p>
        {/if}
      </div>

      <div>
        <button
          type="submit"
          disabled={loading}
          class="w-full rounded-md bg-purple-600 py-2 text-white transition hover:bg-purple-700 disabled:opacity-70"
        >
          {#if loading}
            Processing...
          {:else}
            {isLogin ? 'Sign In' : 'Create Account'}
          {/if}
        </button>
      </div>
    </form>

    <div class="mt-4 text-center text-sm">
      <button
        on:click={toggleForm}
        class="text-purple-600 hover:text-purple-800"
      >
        {isLogin
          ? "Don't have an account? Sign up"
          : 'Already have an account? Sign in'}
      </button>
    </div>
  </div>
</div>
