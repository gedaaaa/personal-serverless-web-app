<script lang="ts">
  const { filter, onFilterChange } = $props<{
    filter: { isCompleted?: boolean };
    onFilterChange: (filter: { isCompleted?: boolean }) => void;
  }>();

  // Handle filter change
  function handleFilterChange(value?: boolean) {
    onFilterChange({ isCompleted: value });
  }
</script>

<div class="mb-6 flex justify-center">
  <div class="inline-flex rounded-md shadow-sm" role="group">
    <button
      type="button"
      class="rounded-l-md border border-gray-200 bg-white px-4 py-2 text-sm font-medium text-gray-900
             hover:bg-gray-100 hover:text-purple-700 focus:z-10 focus:text-purple-700 focus:outline-none
             {filter?.isCompleted === undefined
        ? 'bg-purple-100 text-purple-700'
        : ''}"
      onclick={() => handleFilterChange(undefined)}
    >
      All
    </button>
    <button
      type="button"
      class="border-b border-r border-t border-gray-200 bg-white px-4 py-2 text-sm font-medium text-gray-900
             hover:bg-gray-100 hover:text-purple-700 focus:z-10 focus:text-purple-700 focus:outline-none
             {filter?.isCompleted === false
        ? 'bg-purple-100 text-purple-700'
        : ''}"
      onclick={() => handleFilterChange(false)}
    >
      Incomplete
    </button>
    <button
      type="button"
      class="rounded-r-md border border-gray-200 bg-white px-4 py-2 text-sm font-medium text-gray-900
             hover:bg-gray-100 hover:text-purple-700 focus:z-10 focus:text-purple-700 focus:outline-none
             {filter?.isCompleted === true
        ? 'bg-purple-100 text-purple-700'
        : ''}"
      onclick={() => handleFilterChange(true)}
    >
      Completed
    </button>
  </div>
</div>
