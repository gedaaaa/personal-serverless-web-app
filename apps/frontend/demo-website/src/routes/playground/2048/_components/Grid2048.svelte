<script lang="ts">
  import _ from 'lodash';
  import { flip } from 'svelte/animate';
  import {
    handleKeyDown,
    handleTouchStart,
    handleTouchEnd,
  } from './interaction';
  import { reset } from './store.svelte';
  import { state } from './store.svelte';
  import { CELL_MOVE_DURATION } from './constants';

  function getFlatGrid() {
    return _.flatMap(state.grid, (row) => row);
  }

  function getColorClass(value: number) {
    switch (value) {
      case 0:
        return 'bg-gray-200 text-transparent border-transparent';
      case 2:
        return 'bg-red-200 text-gray-600';
      case 4:
        return 'bg-orange-200 text-red-600';
      case 8:
        return 'bg-yellow-200 text-orange-600';
      case 16:
        return 'bg-lime-200 text-yellow-600';
      case 32:
        return 'bg-green-200 text-lime-600';
      case 64:
        return 'bg-cyan-200 text-green-600';
      case 128:
        return 'bg-blue-200 text-cyan-600';
      case 256:
        return 'bg-violet-200 text-blue-600';
      case 512:
        return 'bg-purple-200 text-violet-600';
      case 1024:
        return 'bg-purple-400 text-purple-100';
      case 2048:
        return 'bg-purple-600 text-white';
      case 4096:
        return 'bg-purple-800 text-white';
      case 8192:
        return 'bg-purple-900 text-white';
      case 16384:
        return 'bg-purple-950 text-white';
      default:
        return 'bg-rose-900 text-black';
    }
  }

  // Action to prevent default touch scrolling behavior
  function preventTouchScroll(node: HTMLElement) {
    const preventDefault = (e: TouchEvent) => e.preventDefault();

    node.addEventListener('touchstart', preventDefault, { passive: false });
    node.addEventListener('touchmove', preventDefault, { passive: false });

    return {
      destroy() {
        node.removeEventListener('touchstart', preventDefault);
        node.removeEventListener('touchmove', preventDefault);
      },
    };
  }
</script>

<svelte:window onkeydown={handleKeyDown} />

<div class="flex h-full w-full flex-col items-center justify-center">
  <div class="flex flex-row items-center justify-center gap-4">
    {#if state.isGameWon}
      <div class="text-2xl font-bold text-purple-600">YOU WIN!</div>
    {/if}
    <button
      disabled={state.isMoving}
      class="rounded-md bg-purple-600 px-4 py-2 text-white"
      onclick={reset}
    >
      Reset
    </button>
  </div>

  <div
    class="my-4 grid grid-cols-4 gap-4 rounded-md border-2 border-gray-200 bg-gray-100 p-4"
    ontouchstart={handleTouchStart}
    ontouchend={handleTouchEnd}
    use:preventTouchScroll
  >
    {#each getFlatGrid() as cell (cell.id)}
      {@const colorClass = getColorClass(cell.value)}
      <div
        animate:flip={{ duration: CELL_MOVE_DURATION }}
        class="flex h-10 w-10 items-center justify-center rounded-md {colorClass}"
      >
        <div class="break-all text-center leading-none">{cell.value}</div>
      </div>
    {/each}
  </div>
</div>
