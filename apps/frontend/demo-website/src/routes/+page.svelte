<script lang="ts">
    import {ApiClient} from "$lib/api";

    let error: string | null = null;

    let greeting: string;
    const api = new ApiClient();

    const sayHello = async () => {
        try {
            const response = await api.get<{message: string}>('/hello-world/greeting');
            greeting = response.message
        } catch {
            error = 'Server is quite mad now.';
        }
    }

</script>

<div class="mx-auto my-24 text-center">
    {#if error}
        <div class="error text-red-500">{error}</div>
    {:else if greeting}
        <p class="text-gray-700">Server says: <span class="text-purple-600">{greeting}</span></p>
    {:else }
        <div>
            <p class="text-gray-700">Do you want to say hello?</p>
            <button class="border-2 rounded-full bg-slate-300 px-4 py-1 min-w-32 my-8" on:click={sayHello}>Hi!</button>
        </div>
    {/if}
</div>

