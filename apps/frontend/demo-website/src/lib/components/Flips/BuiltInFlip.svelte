<script lang="ts">
  import { flip } from 'svelte/animate';
  import _ from 'lodash';

  var items = $state(_.range(20));

  function shuffle() {
    items = _.shuffle(items);
  }
</script>

<div class="flex flex-col items-center justify-center">
  <button
    class="mx-4 my-4 rounded-md bg-purple-600 px-4 py-2 text-white transition hover:bg-purple-700"
    onclick={shuffle}>Shuffle</button
  >
  <div class="grid grid-cols-4 gap-4">
    {#each items as item (item)}
      <div
        class="flex h-16 w-16 shrink-0 items-center justify-center rounded-md bg-gray-200"
        animate:flip={{ duration: 750 }}
      >
        <div>
          {item}
        </div>
      </div>
    {/each}
  </div>
</div>
