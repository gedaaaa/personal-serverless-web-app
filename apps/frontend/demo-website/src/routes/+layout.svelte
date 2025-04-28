<script lang="ts">
  import '../app.css';
  import { Header } from '$lib/components/Header';
  let { children } = $props();
</script>

<Header />

<main class="min-w-screen mx-auto min-h-screen bg-gray-50 py-8">
  {@render children()}
</main>
