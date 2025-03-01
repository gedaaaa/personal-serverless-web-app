<script lang="ts">
  import { ApiClient } from '$lib/api';
  import { auth, logout } from '$lib/auth';

  let error: string | null = null;
  let greeting: string;
  const api = new ApiClient();

  const sayHello = async () => {
    try {
      const response = await api.get<{ message: string }>(
        '/hello-world/greeting',
      );
      greeting = response.message;
    } catch {
      error = 'Server is quite mad now!';
    }
  };

  const handleLogout = () => {
    // Logout and redirect to home page
    logout('/');
  };
</script>

<div class="mx-auto my-12 max-w-md px-4">
  <div class="mb-6 flex items-center justify-between">
    <h1 class="text-2xl font-medium text-gray-800">Hello World</h1>
    <button
      class="rounded-md bg-gray-200 px-4 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-300"
      on:click={handleLogout}
    >
      Logout
    </button>
  </div>

  {#if $auth.user}
    <p class="mb-6 text-gray-600">
      Welcome, <span class="font-medium">{$auth.user.username}</span>!
    </p>
  {/if}

  <div class="rounded-lg bg-white p-6 shadow-sm">
    {#if error}
      <div class="mb-4 rounded-md bg-red-50 p-4 text-red-600">{error}</div>
    {:else if greeting}
      <p class="text-gray-700">
        Server says: <span class="font-medium text-purple-600">{greeting}</span>
      </p>
    {:else}
      <div>
        <p class="text-gray-700">Do you want to say hello?</p>
        <button
          class="mt-4 rounded-md bg-purple-600 px-4 py-2 text-white transition hover:bg-purple-700"
          on:click={sayHello}>Say Hello</button
        >
      </div>
    {/if}
  </div>
</div>
