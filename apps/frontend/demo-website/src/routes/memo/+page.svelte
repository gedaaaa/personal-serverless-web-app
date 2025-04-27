<script lang="ts">
  import { onMount } from 'svelte';
  import { type Memo } from './_services/memo-service';
  import { store, applyFilter } from './_stores/memoStore.svelte';
  import MemoCard from './_components/MemoCard.svelte';
  import MemoModal from './_components/MemoModal.svelte';
  import MemoTab from './_components/MemoTab.svelte';
  import DeleteConfirmModal from './_components/DeleteConfirmModal.svelte';
  import AddMemoButton from './_components/AddMemoButton.svelte';
  import InfiniteScroll from './_components/InfiniteScroll.svelte';
  import { flip } from 'svelte/animate';
  import { quintOut } from 'svelte/easing';
  import { goto } from '$app/navigation';
  import { auth } from '$lib/auth';

  // --- Local State for Modal Management ---
  let openModalType: 'create' | 'edit' | 'delete' | undefined =
    $state(undefined);
  let activeMemo: Memo | undefined = $state(undefined);

  // --- Modal Control Functions ---
  function openModal(type: 'create' | 'edit' | 'delete', memo?: Memo) {
    openModalType = type;
    activeMemo = memo;
  }
  function closeModal() {
    openModalType = undefined;
    activeMemo = undefined;
  }

  // --- Initial Data Fetch ---
  onMount(() => {
    if (!$auth.isAuthenticated) {
      goto('/login?location=/memo');
    }
  });
</script>

<div class="mx-auto my-8 max-w-4xl px-4">
  <!-- Header -->
  <div class="mb-8 flex items-center justify-between">
    <h1 class="text-2xl font-medium text-gray-800">My Memos</h1>

    <!-- Add button Component -->
    <AddMemoButton onAddClick={() => openModal('create')} />
  </div>

  <!-- Tab -->
  <MemoTab onTabChange={applyFilter} />

  <!-- Error message from store -->
  {#if store.error}
    <div class="mb-4 rounded-md bg-red-50 p-4 text-red-600">
      {store.error}
      <!-- Consider adding a close button that calls memoStore.setError(undefined) -->
    </div>
  {/if}

  <!-- Memo list -->
  <div class="space-y-4">
    {#each store.memos as memo (memo.id)}
      <!-- Pass loading status from store -->
      <div
        class="transition-opacity will-change-transform {store
          .itemLoadingStatus[memo.id]
          ? 'opacity-50'
          : ''}"
        animate:flip={{ duration: 750, easing: quintOut }}
      >
        <MemoCard
          {memo}
          loading={store.itemLoadingStatus[memo.id] ?? false}
          onEdit={() => openModal('edit', memo)}
          onDelete={() => openModal('delete', memo)}
        />
      </div>
    {/each}
  </div>

  <!-- Infinite Scroll Component -->
  <InfiniteScroll />

  <!-- Edit/Create Modal -->
  <MemoModal
    show={openModalType === 'create' || openModalType === 'edit'}
    memo={activeMemo}
    onClose={closeModal}
  />

  <!-- Delete Confirmation Modal -->
  <DeleteConfirmModal
    show={openModalType === 'delete'}
    memo={activeMemo}
    onClose={closeModal}
  />
</div>
