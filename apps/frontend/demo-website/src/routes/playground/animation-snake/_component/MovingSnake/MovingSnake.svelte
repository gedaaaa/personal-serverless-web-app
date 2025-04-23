<script lang="ts">
  import _ from 'lodash';
  import { onDestroy, onMount } from 'svelte';
  import { SVG_VIEW_BOX_X, SVG_VIEW_BOX_Y } from './constants';
  import { state } from './store.svelte';
  import { manageSnakeMovementLoop } from './logic/movement.svelte';
  import { managePlaceBaitTimer } from './logic/bait.svelte';

  onMount(() => {
    state.isAnimationRunning = true;
  });

  manageSnakeMovementLoop();
  managePlaceBaitTimer();

  onDestroy(() => {
    state.isAnimationRunning = false;
  });
</script>

<div class="flex h-full w-full flex-col items-center justify-center gap-4">
  <div class="w-3/4 bg-gray-200">
    <svg
      width="100%"
      height="100%"
      viewBox={`0 0 ${SVG_VIEW_BOX_X} ${SVG_VIEW_BOX_Y}`}
    >
      <g style="transform: translateZ(0)">
        {#each { length: state.snakeDots.length } as _, i}
          {@const reversedIndex = state.snakeDots.length - 1 - i}
          {@const dot = state.snakeDots[reversedIndex]}
          {#if reversedIndex < state.snakeDots.length - 1}
            {@const prevDotInReversed = state.snakeDots[reversedIndex + 1]}
            <line
              x1={dot.x}
              y1={dot.y}
              x2={prevDotInReversed.x}
              y2={prevDotInReversed.y}
              stroke="gray"
              stroke-width="2"
            />
          {/if}
          <circle
            cx={dot.x}
            cy={dot.y}
            r="8"
            fill={reversedIndex === 0 ? 'black' : 'white'}
            stroke="gray"
            stroke-width="1"
          />
        {/each}
      </g>
    </svg>
  </div>
  <button
    class="w-1/4 rounded-md bg-gray-200 p-2"
    on:click={() => (state.isAnimationRunning = !state.isAnimationRunning)}
  >
    {state.isAnimationRunning ? 'Pause' : 'Start'}
  </button>
</div>
