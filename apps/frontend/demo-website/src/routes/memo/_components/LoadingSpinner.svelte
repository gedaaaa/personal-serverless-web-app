<script lang="ts">
  const { size = 'md' }: { size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' } =
    $props();

  const sizeClass = {
    sm: 'h-5 w-5 border-2 border-purple-600',
    md: 'h-8 w-8 border-2 border-purple-600',
    lg: 'h-12 w-12 border-4 border-purple-500',
    xl: 'h-16 w-16 border-4 border-purple-500',
    '2xl': 'h-20 w-20 border-8 border-purple-400',
  }[size];
</script>

<div class="flex items-center justify-center">
  <div
    class={`animate-spin rounded-full border-solid border-t-transparent ${sizeClass}`}
  ></div>
</div>
