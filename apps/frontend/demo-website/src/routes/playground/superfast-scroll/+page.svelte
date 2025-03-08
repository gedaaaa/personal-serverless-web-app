<script lang="ts">
  import VirtualScrollListDemo from './_components/';
</script>

<div class="mx-auto max-w-4xl p-5">
  <h1 class="mb-2.5 text-center">Virtual Scroll List Demo</h1>
  <p class="mb-8 text-center text-gray-600">
    High-performance virtual scroll list implemented with
    RingBufferVisibleItemsProvider
  </p>

  <div class="mb-10">
    <VirtualScrollListDemo />
  </div>

  <div class="rounded-lg bg-gray-100 p-5">
    <h2 class="mb-4 mt-0">Implementation Details</h2>
    <p>This virtual scroll list uses the following technologies:</p>
    <ul class="ml-5">
      <li class="mb-2">
        SkipListDataSource as an efficient data source with O(log n) operations
      </li>
      <li class="mb-2">
        RingBufferVisibleItemsProvider to manage visible items and buffer zones
      </li>
      <li class="mb-2">
        Reactive updates through effect hooks monitoring provider version
        changes
      </li>
      <li class="mb-2">
        Support for direct position jumping with efficient rendering
      </li>
      <li class="mb-2">
        Circular bidirectional linked list for optimal memory management
      </li>
      <li class="mb-2">
        Debounced position updates to prevent rendering thrashing
      </li>
    </ul>

    <h3 class="mb-3 mt-5">DOM Rendering Optimization</h3>
    <p class="mb-3">
      The viewport rendering system employs several advanced techniques:
    </p>
    <ul class="ml-5">
      <li class="mb-2">
        Fixed DOM pool - Reuses a constant number of DOM elements which will not
        be destroy/recreate when items change
      </li>
      <li class="mb-2">
        Circular DOM recycling - Uses a virtual ring head to efficiently
        reposition DOM elements
      </li>
      <li class="mb-2">
        CSS transforms - Leverages hardware-accelerated transforms for smooth
        scrolling
      </li>
      <li class="mb-2">
        Synchronized updates - Updates both visual position and data items in
        the same render tick to prevent double rendering or re-painting
      </li>
      <li class="mb-2">
        Performance attributes - Uses will-change and other optimizations for
        better rendering performance
      </li>
    </ul>
  </div>
</div>
