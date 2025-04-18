<script lang="ts">
  import memoService, { type Memo } from '../_services/memo-service';
  import {
    setError,
    setItemLoadingStatus,
    removeMemoFromList,
  } from '../_stores/memoStore.svelte';

  // Component props
  const {
    show = $bindable<boolean>(false),
    memo,
    onClose,
  } = $props<{
    show: boolean;
    memo?: Memo;
    onClose: () => void;
  }>();

  let isDeleting = $state(false);

  // Reset deleting state when modal opens
  $effect(() => {
    if (show) {
      isDeleting = false;
    }
  });

  /**
   * Handles the deletion of current memo.
   */
  async function handleDelete() {
    if (!memo || isDeleting) return;

    const memoId = memo.id;

    isDeleting = true;
    setItemLoadingStatus(memoId, true);
    setError(undefined); // Clear previous errors

    try {
      await memoService.deleteMemo(memoId);
      removeMemoFromList(memoId); // Update store
      onClose(); // Close modal on success
    } catch (err) {
      console.error('Failed to delete memo:', err);
      setError('Failed to delete memo. Please try again.');
      // Do not close modal on error
    } finally {
      // Ensure loading status is cleared even if component closes before API returns
      // Check if memo still exists in props before trying to clear its loading state
      if (memo?.id === memoId) {
        setItemLoadingStatus(memoId, false);
      }
      isDeleting = false;
    }
  }

  function handleCancel() {
    if (isDeleting) return;
    onClose();
  }
</script>

{#if show && memo}
  <div class="fixed inset-0 z-10 overflow-y-auto">
    <div
      class="flex min-h-screen items-end justify-center px-4 pb-20 pt-4 text-center sm:block sm:p-0"
    >
      <!-- Background overlay -->
      <div class="fixed inset-0 transition-opacity" aria-hidden="true">
        <div class="absolute inset-0 bg-gray-500 opacity-75"></div>
      </div>

      <!-- Modal panel -->
      <div
        class="inline-block transform overflow-hidden rounded-lg bg-white text-left align-bottom shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:align-middle"
      >
        <div class="bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
          <div class="sm:flex sm:items-start">
            <div
              class="mx-auto flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-red-100 sm:mx-0 sm:h-10 sm:w-10"
            >
              <svg
                class="h-6 w-6 text-red-600"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                aria-hidden="true"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            </div>
            <div class="mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left">
              <h3 class="text-lg font-medium leading-6 text-gray-900">
                Delete Memo
              </h3>
              <div class="mt-2">
                <p class="text-sm text-gray-500">
                  Are you sure you want to delete the memo "<strong
                    class="font-medium">{memo.title}</strong
                  >"? This action cannot be undone.
                </p>
              </div>
            </div>
          </div>
        </div>
        <div class="bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6">
          <button
            type="button"
            onclick={handleDelete}
            disabled={isDeleting}
            class="inline-flex w-full justify-center rounded-md border border-transparent bg-red-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 sm:ml-3 sm:w-auto sm:text-sm"
          >
            {#if isDeleting}
              <svg
                class="mr-2 h-5 w-5 animate-spin text-white"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  class="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  stroke-width="4"
                ></circle>
                <path
                  class="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                ></path>
              </svg>
              Deleting...
            {:else}
              Delete
            {/if}
          </button>
          <button
            type="button"
            onclick={handleCancel}
            disabled={isDeleting}
            class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 sm:mt-0 sm:w-auto sm:text-sm"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  </div>
{/if}
