<script lang="ts">
  import type { DataItem } from '../../data/DataSource/DataSource';
  import type { VisibleItemsProvider } from '../../data/VisibleItemsProvider';

  // Props
  let {
    currentPosition = 0,
    totalCount = 0,
    isAtStart = true,
    isAtEnd = false,
    provider = $bindable<VisibleItemsProvider<DataItem> | null>(null),
    onJumpToPosition = $bindable<(position: number) => void>(),
  } = $props();

  // Jump to first position
  function handleJumpToFirst() {
    if (!provider) return;
    onJumpToPosition(provider.getFirstValidPosition());
  }

  // Jump to previous position
  function handleJumpToPrevious() {
    onJumpToPosition(Math.max(0, currentPosition - 1));
  }

  // Jump to next position
  function handleJumpToNext() {
    if (!provider) return;
    onJumpToPosition(
      Math.min(provider.getLastValidPosition(), currentPosition + 1),
    );
  }

  // Jump to last position
  function handleJumpToLast() {
    if (!provider) return;
    onJumpToPosition(provider.getLastValidPosition());
  }
</script>

<div
  class="flex items-center justify-between border-t border-gray-200 bg-gray-50 p-2"
>
  <div class="text-sm text-gray-600">
    Position: {currentPosition} / {totalCount - 1}
  </div>
  <div class="flex gap-2">
    <button
      onclick={handleJumpToFirst}
      disabled={isAtStart}
      class="rounded border border-gray-300 bg-white px-2 py-1 text-sm transition-colors hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
    >
      First
    </button>
    <button
      onclick={handleJumpToPrevious}
      disabled={isAtStart}
      class="rounded border border-gray-300 bg-white px-2 py-1 text-sm transition-colors hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
    >
      Previous
    </button>
    <button
      onclick={handleJumpToNext}
      disabled={isAtEnd}
      class="rounded border border-gray-300 bg-white px-2 py-1 text-sm transition-colors hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
    >
      Next
    </button>
    <button
      onclick={handleJumpToLast}
      disabled={isAtEnd}
      class="rounded border border-gray-300 bg-white px-2 py-1 text-sm transition-colors hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
    >
      Last
    </button>
  </div>
</div>
