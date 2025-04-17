<script lang="ts">
  import { onMount } from 'svelte';
  import memoService, {
    type Memo,
    type UpdateMemoRequest,
    type CreateMemoRequest,
    type GetMemoListFilter,
  } from './_services/memo-service';
  import MemoCard from './_components/MemoCard.svelte';
  import MemoModal from './_components/MemoModal.svelte';
  import MemoFilter from './_components/MemoFilter.svelte';
  import DeleteConfirmModal from './_components/DeleteConfirmModal.svelte';
  import LoadingSpinner from './_components/LoadingSpinner.svelte';

  // State management
  let memos = $state<Memo[]>([]);
  let loadingItems = $state<Record<string, boolean>>({});
  let error = $state<string | undefined>(undefined);
  let cursor = $state<string | undefined>(undefined);
  let hasMore = $state(true);
  let filter = $state<GetMemoListFilter>({});
  let isFetchingMemos = $state(false);
  let isJustSwitchedTab = $state(true);
  let pendingFetchRequest = $state(false);

  // Modal state
  let showModal = $state(false);
  let showDeleteModal = $state(false);
  let currentMemo = $state<Memo | undefined>(undefined);
  let memoToDelete = $state<Memo | undefined>(undefined);

  // Sentinel element for IntersectionObserver
  let sentinelElement: HTMLDivElement;

  // Fetch memos with pagination
  async function fetchMemos() {
    if (isFetchingMemos) {
      pendingFetchRequest = true;
      return;
    }

    if (!isJustSwitchedTab && !hasMore) {
      return;
    }

    isFetchingMemos = true;

    try {
      error = undefined;

      if (isJustSwitchedTab) {
        cursor = undefined;
        memos = [];
        hasMore = true;
      }

      const response = await memoService.getMemos(10, cursor, filter);
      if (isJustSwitchedTab) {
        memos = response.items ?? [];
      } else {
        memos = [...memos, ...(response.items ?? [])];
      }

      cursor = response.nextCursor;
      hasMore = !!response.nextCursor;
    } catch (err) {
      error = 'Failed to load memos. Please try again.';
      console.error('Error fetching memos:', err);
    } finally {
      isFetchingMemos = false;
      if (pendingFetchRequest) {
        pendingFetchRequest = false;
        await fetchMemos();
      }
      isJustSwitchedTab = false;
    }
  }

  // Helper to set loading state for specific memo
  function setItemLoading(id: string, isLoading: boolean) {
    loadingItems[id] = isLoading;
  }

  // Create a new memo
  async function handleCreateMemo(data: CreateMemoRequest) {
    try {
      await memoService.createMemo(data);
      showModal = false;
      await fetchMemos(true);
    } catch (err) {
      error = 'Failed to create memo. Please try again.';
      console.error('Error creating memo:', err);
    }
  }

  // Update a memo
  async function handleUpdateMemo(id: string, data: UpdateMemoRequest) {
    setItemLoading(id, true);

    try {
      await memoService.updateMemo(id, data);

      // Update memo in the list without refetching
      memos = memos.map((memo) =>
        memo.id === id ? { ...memo, ...data } : memo,
      );

      showModal = false;
    } catch (err) {
      error = 'Failed to update memo. Please try again.';
      console.error('Error updating memo:', err);
    } finally {
      setItemLoading(id, false);
    }
  }

  // Toggle memo complete status
  async function handleToggleComplete(id: string, completed: boolean) {
    setItemLoading(id, true);

    try {
      const memo = memos.find((m) => m.id === id);
      if (!memo) return;

      const updateData: UpdateMemoRequest = {
        title: memo.title,
        content: memo.content,
        reminderTime: memo.reminderTime,
        isCompleted: completed,
      };

      await memoService.updateMemo(id, updateData);

      // Update memo in the list without refetching
      memos = memos.map((memo) =>
        memo.id === id ? { ...memo, isCompleted: completed } : memo,
      );
    } catch (err) {
      error = 'Failed to update memo status. Please try again.';
      console.error('Error updating memo status:', err);
    } finally {
      setItemLoading(id, false);
    }
  }

  // Show delete confirmation modal
  function handleConfirmDelete(memo: Memo) {
    memoToDelete = memo;
    showDeleteModal = true;
  }

  // Delete a memo after confirmation
  async function handleDeleteMemo() {
    if (!memoToDelete) return;

    const id = memoToDelete.id;
    setItemLoading(id, true);

    try {
      await memoService.deleteMemo(id);

      // Remove memo from the list
      memos = memos.filter((memo) => memo.id !== id);

      // Close delete modal
      showDeleteModal = false;
      memoToDelete = undefined;
    } catch (err) {
      error = 'Failed to delete memo. Please try again.';
      console.error('Error deleting memo:', err);
    } finally {
      setItemLoading(id, false);
    }
  }

  // Open edit modal
  function handleEditMemo(memo: Memo) {
    currentMemo = memo;
    showModal = true;
  }

  // Open create modal
  function handleAddMemo() {
    currentMemo = undefined;
    showModal = true;
  }

  // Handle filter change
  async function handleFilterChange(newFilter: GetMemoListFilter) {
    isJustSwitchedTab = true;
    filter = newFilter;
    // Reset state for new filter
    cursor = undefined;
    memos = [];
    hasMore = true;
    await fetchMemos(); // Fetch initial data for the new filter
  }

  // Handle modal save
  function handleModalSave(data: CreateMemoRequest | UpdateMemoRequest) {
    if (currentMemo) {
      handleUpdateMemo(currentMemo.id, data as UpdateMemoRequest);
    } else {
      handleCreateMemo(data as CreateMemoRequest);
    }
  }

  // Close delete modal
  function handleCancelDelete() {
    showDeleteModal = false;
    memoToDelete = undefined;
  }

  onMount(() => {
    // Manually fetch initial data if needed (or rely on filter change)
    fetchMemos();

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && hasMore && !isFetchingMemos) {
          fetchMemos();
        }
      },
      {
        rootMargin: '0px 0px 200px 0px', // Start loading 200px before the element is visible
      },
    );

    // Ensure the sentinel element exists before observing
    const observeSentinel = () => {
      if (sentinelElement) {
        observer.observe(sentinelElement);
      } else {
        // If sentinel is not rendered yet (e.g. due to #if hasMore / !isJustSwitchedTab), try again shortly
        requestAnimationFrame(observeSentinel);
      }
    };
    observeSentinel();

    // Cleanup observer on component destroy
    return () => {
      observer.disconnect();
    };
  });
</script>

<div class="mx-auto my-8 max-w-4xl px-4">
  <!-- Header -->
  <div class="mb-8 flex items-center justify-between">
    <h1 class="text-2xl font-medium text-gray-800">My Memos</h1>

    <!-- Add button -->
    <button
      onclick={handleAddMemo}
      class="flex items-center rounded-full bg-purple-600 p-3 text-white shadow-sm hover:bg-purple-700 focus:outline-none"
      aria-label="Add new memo"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        class="h-6 w-6"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M12 6v6m0 0v6m0-6h6m-6 0H6"
        />
      </svg>
    </button>
  </div>

  <!-- Filter -->
  <MemoFilter {filter} onFilterChange={handleFilterChange} />

  <!-- Error message -->
  {#if error}
    <div class="mb-4 rounded-md bg-red-50 p-4 text-red-600">
      {error}
    </div>
  {/if}

  <!-- Memo list -->
  <div class="space-y-4">
    {#each memos as memo (memo.id)}
      <div
        class="transition-opacity {loadingItems[memo.id] ? 'opacity-50' : ''}"
      >
        <MemoCard
          {memo}
          onToggleComplete={handleToggleComplete}
          onEdit={handleEditMemo}
          onDelete={() => {}}
          onConfirmDelete={handleConfirmDelete}
        />
      </div>
    {/each}
  </div>
  <!-- Loading indicator for list loads -->
  {#if isFetchingMemos}
    <div class="py-4">
      <LoadingSpinner size="md" />
    </div>
  {/if}

  <!-- Sentinel Element for IntersectionObserver -->
  <div bind:this={sentinelElement} class="h-1"></div>

  <!-- Message when no more items -->
  {#if !hasMore && memos.length > 0}
    <p class="py-4 text-center text-gray-500">No more memos found.</p>
  {/if}

  <!-- Message when no items match filter -->
  {#if !hasMore && memos.length === 0 && !isFetchingMemos && !isJustSwitchedTab}
    <p class="py-4 text-center text-gray-500">
      No memos match the current filter.
    </p>
  {/if}

  <!-- Edit/Create Modal -->
  <MemoModal
    show={showModal}
    memo={currentMemo}
    onSave={handleModalSave}
    onClose={() => (showModal = false)}
  />

  <!-- Delete Confirmation Modal -->
  <DeleteConfirmModal
    show={showDeleteModal}
    memo={memoToDelete}
    onConfirm={handleDeleteMemo}
    onCancel={handleCancelDelete}
  />
</div>
