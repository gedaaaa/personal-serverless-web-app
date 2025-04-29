<script lang="ts">
  import { getDefaultClient } from '$lib/api/client';
  import { auth } from '$lib/auth';

  let error = $state<string | null>(null);
  let greeting = $state<string | undefined>(undefined);

  const sayHello = async () => {
    try {
      const api = getDefaultClient();
      const response = await api.get<{ message: string }>(
        '/hello-world/v1/greeting',
      );
      greeting = response.message;
    } catch {
      error = 'Server is quite mad now!';
    }
  };
</script>

<div class="mx-auto my-12 max-w-md px-4">
  <div class="mb-6 flex items-center justify-between">
    <h1 class="text-2xl font-medium text-gray-800">Hello World</h1>
  </div>
  <p class="mb-6 text-gray-600">
    Welcome,
    {#if auth.user}
      <span class="font-medium">{auth.user.username}</span>!
    {/if}
  </p>
  <div class="rounded-lg bg-white p-6 shadow-sm">
    {#if error}
      <div
        class="mb-4 rounded-md bg-red-50 p-4 text-red-600"
        data-testid="error-message"
      >
        {error}
      </div>
    {:else if greeting}
      <p class="text-gray-700" data-testid="server-response">
        Server says: <span class="font-medium text-purple-600">{greeting}</span>
      </p>
      <button
        class="mt-4 rounded-md bg-purple-600 px-4 py-2 text-white transition hover:bg-purple-700"
        onclick={sayHello}>Say Hello Again</button
      >
    {:else}
      <div>
        <p class="text-gray-700">Do you want to say hello?</p>
        <button
          class="mt-4 rounded-md bg-purple-600 px-4 py-2 text-white transition hover:bg-purple-700"
          onclick={sayHello}>Say Hello</button
        >
      </div>
    {/if}
  </div>
</div>
