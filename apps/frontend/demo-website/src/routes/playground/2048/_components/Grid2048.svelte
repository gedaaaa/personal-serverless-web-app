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
        return 'bg-blue-200 text-blue-400';
      case 4:
        return 'bg-green-200 text-green-400';
      case 8:
        return 'bg-yellow-200 text-yellow-400';
      case 16:
        return 'bg-orange-200 text-orange-400';
      case 32:
        return 'bg-red-200 text-red-400';
      case 64:
        return 'bg-purple-200 text-purple-400';
      case 128:
        return 'bg-pink-200 text-pink-400';
      case 256:
        return 'bg-brown-200 text-brown-400';
      case 512:
        return 'bg-gray-200 text-gray-400';
      case 1024:
        return 'bg-gray-400 text-gray-600';
      case 2048:
        return 'bg-gray-600 text-gray-100';
      default:
        return 'bg-gray-200 text-gray-400';
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

<svelte:window on:keydown={handleKeyDown} />

<div class="flex h-full w-full flex-col items-center justify-center">
  <div class="flex flex-row items-center justify-center gap-4">
    {#if state.isGameWon}
      YOU WIN!
    {/if}
    <button
      disabled={state.isMoving}
      class="rounded-md bg-blue-500 px-4 py-2 text-white"
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
        <div>{cell.value}</div>
      </div>
    {/each}
  </div>
</div>
