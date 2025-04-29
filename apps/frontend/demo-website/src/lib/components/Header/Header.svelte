<script lang="ts">
  import { onMount } from 'svelte';
  import { auth, logout } from '$lib/auth/auth.svelte';
  import { page } from '$app/state';

  let showShadow = $state(false);

  const currentUrl = $derived(
    page.url.pathname ||
      '' + page.url.search ||
      '' + page.url.hash ||
      '' ||
      '/',
  );

  const encodedCurrentUrl = $derived(encodeURIComponent(currentUrl));

  const isLoginPage = $derived(page.url?.pathname === '/login');

  // Handle scroll event to add shadow when scrolling
  const handleScroll = () => {
    showShadow = window.scrollY > 0;
  };

  onMount(() => {
    // Add scroll event listener
    window.addEventListener('scroll', handleScroll);

    // Initialize shadow state
    handleScroll();

    // Clean up on component unmount
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  });

  // Get the first letter of username for avatar
  const userInitial = $derived(
    auth.user?.username.charAt(0).toUpperCase() || '',
  );

  // Generate a consistent background color based on username
  const avatarBgColor = $derived(
    auth.user
      ? `hsl(${(auth.user.username.charCodeAt(0) * 10) % 360}, 70%, 60%)`
      : 'hsl(210, 70%, 60%)',
  );

  // Handle logout
  const handleLogout = () => {
    logout(currentUrl);
  };

  // Handle login
  const goToLogin = () => {
    window.location.href = '/login?location=' + encodedCurrentUrl;
  };
</script>

<header
  class="fixed left-0 right-0 top-0 z-50 h-14 bg-gray-50 px-4 py-3 transition-shadow duration-300 md:px-6"
  class:shadow-md={showShadow}
>
  <div class="container mx-auto flex items-center justify-between">
    <a href="/" class="text-xl font-semibold text-gray-800">Welcome</a>

    <div class="flex items-center gap-3">
      {#if auth.isAuthenticated}
        <div class="flex items-center gap-2">
          <div
            class="flex h-8 w-8 items-center justify-center rounded-full font-medium text-white"
            style="background-color: {avatarBgColor};"
          >
            {userInitial}
          </div>
          <span class="hidden text-sm font-medium text-gray-700 md:inline"
            >{auth.user?.username}</span
          >
        </div>

        <button
          onclick={handleLogout}
          class="rounded bg-gray-200 px-3 py-1.5 text-sm font-medium text-gray-800 transition-colors hover:bg-gray-200"
        >
          Logout
        </button>
      {:else if !isLoginPage}
        <button
          onclick={goToLogin}
          class="rounded bg-gray-200 px-3 py-1.5 text-sm font-medium text-gray-800 transition-colors hover:bg-gray-200"
        >
          Sign In
        </button>
      {/if}
    </div>
  </div>
</header>

<!-- Spacer to push content below the fixed header -->
<div class="h-14"></div>
