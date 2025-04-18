<script lang="ts">
  const {
    onTabChange,
  }: { onTabChange: (filter: { isCompleted: boolean }) => void } = $props();

  const tabs = ['Incomplete', 'Completed'] as const;
  type Tab = (typeof tabs)[number];

  let selectedTab = $state<Tab>('Incomplete');

  function getTabClass(tab: Tab): string {
    return selectedTab === tab ? 'bg-purple-100 text-purple-700' : '';
  }

  function getTabFilter(tab: Tab): { isCompleted: boolean } {
    switch (tab) {
      case 'Incomplete':
        return { isCompleted: false };
      case 'Completed':
        return { isCompleted: true };
    }
  }

  // Handle tab change
  function handleTabChange(tab: Tab) {
    selectedTab = tab;
    onTabChange(getTabFilter(tab));
  }
</script>

<div class="mb-6 flex justify-center">
  <div class="inline-flex rounded-md shadow-sm" role="group">
    {#each tabs as tab}
      <button
        type="button"
        class="border border-gray-200 bg-white px-4 py-2 text-sm font-medium text-gray-900
        first:rounded-l-md last:rounded-r-md
             hover:bg-gray-100 hover:text-purple-700 focus:z-10 focus:text-purple-700 focus:outline-none
             {getTabClass(tab)}"
        onclick={() => handleTabChange(tab)}
      >
        {tab}
      </button>
    {/each}
  </div>
</div>
