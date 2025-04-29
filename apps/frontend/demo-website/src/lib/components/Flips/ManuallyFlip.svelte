<script lang="ts">
  import _ from 'lodash';
  import { tick } from 'svelte';

  var items = $state(_.range(20));
  let elementRefs: Record<number, HTMLDivElement> = $state({});
  let fromMemo: Record<number, DOMRect> = {};
  let toMemo: Record<number, DOMRect> = {};

  /**
   * Shuffle the items with FLIP animation
   */
  async function shuffle() {
    first();
    items = _.shuffle(items);
    await tick();
    last();
    invert();
    requestAnimationFrame(play);
  }

  /**
   * Record the initial position of the items
   */
  function first() {
    items.forEach((item) => {
      const el = elementRefs[item];
      if (el) {
        fromMemo[item] = el.getBoundingClientRect();
      }
    });
  }

  /**
   * Record the final position of the items
   */
  function last() {
    items.forEach((item) => {
      const el = elementRefs[item];
      if (el) {
        toMemo[item] = el.getBoundingClientRect();
      }
    });
  }

  /**
   * Invert the items position by transform to create a illusion of items not moved
   */
  function invert() {
    for (const item of items) {
      const el = elementRefs[item];
      if (el) {
        const invertTransform = calculateInvertTransform(
          fromMemo[item],
          toMemo[item],
        );
        el.style.transition = '';
        el.style.transform = invertTransform;
      }
    }
  }

  /**
   * Play the animation by removing the transform and adding a transition
   */
  function play() {
    for (const item of items) {
      const el = elementRefs[item];
      if (el) {
        el.style.transform = '';
        el.style.transition = 'transform 0.75s ease-in-out';
      }
    }
  }

  function calculateInvertTransform(from: DOMRect, to: DOMRect) {
    const invertX = from.left - to.left;
    const invertY = from.top - to.top;
    return `translate(${invertX}px, ${invertY}px)`;
  }
</script>

<div class="flex flex-col items-center justify-center">
  <button
    class="mx-4 my-4 rounded-md bg-purple-600 px-4 py-2 text-white transition hover:bg-purple-700"
    onclick={shuffle}
  >
    Shuffle
  </button>
  <div class="grid grid-cols-4 gap-4">
    {#each items as item (item)}
      <div
        class="flex h-16 w-16 shrink-0 items-center justify-center rounded-md bg-gray-200"
        bind:this={elementRefs[item]}
      >
        <div>
          {item}
        </div>
      </div>
    {/each}
  </div>
</div>
