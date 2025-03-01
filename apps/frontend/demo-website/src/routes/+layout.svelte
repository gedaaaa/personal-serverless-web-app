<script lang="ts">
  import '../app.css';
  let { children } = $props();
</script>

<main class="min-w-screen mx-auto min-h-screen bg-gray-50 py-8">
  {@render children()}
</main>
