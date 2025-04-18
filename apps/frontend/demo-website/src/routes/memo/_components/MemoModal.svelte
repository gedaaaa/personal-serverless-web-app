<script lang="ts">
  import memoService, {
    type Memo,
    type CreateMemoRequest,
    type UpdateMemoRequest,
  } from '../_services/memo-service';
  import memoStore from '../_stores/memoStore.svelte.ts';

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

  // Form state
  let title = $state('');
  let content = $state('');
  let reminderTime = $state('');
  let isCompleted = $state(false);
  let isSaving = $state(false);

  const { tryFetchNextMemo } = memoStore;

  // Reset form when memo or show status changes
  $effect(() => {
    if (show) {
      isSaving = false; // Reset saving state when modal opens
      if (memo) {
        title = memo.title;
        content = memo.content;
        isCompleted = memo.isCompleted;

        // Convert ISO string to local datetime-local format if it exists
        if (memo.reminderTime) {
          try {
            const date = new Date(memo.reminderTime);
            // Check if date is valid before formatting
            if (!isNaN(date.getTime())) {
              reminderTime = new Date(
                date.getTime() - date.getTimezoneOffset() * 60000,
              )
                .toISOString()
                .slice(0, 16);
            } else {
              reminderTime = ''; // Set to empty if invalid
            }
          } catch {
            reminderTime = ''; // Set to empty on error
          }
        } else {
          reminderTime = '';
        }
      } else {
        // Reset for create mode
        title = '';
        content = '';
        reminderTime = '';
        isCompleted = false;
      }
    }
  });

  // Handle form submission - Now includes API call and store update
  async function handleSubmit(e: Event) {
    e.preventDefault();
    if (isSaving) return; // Prevent double submission

    isSaving = true;
    // Use a temporary ID for loading status during creation if needed
    const loadingId = memo?.id ?? 'creating';
    memoStore.setItemLoadingStatus(loadingId, true);
    memoStore.setError(undefined); // Clear previous errors

    const requestData: CreateMemoRequest | UpdateMemoRequest = {
      title: title.trim() || 'No Title',
      content: content.trim() || 'No Content',
      reminderTime: reminderTime
        ? new Date(reminderTime).toISOString()
        : undefined,
      // Only include isCompleted for updates (when memo exists)
      ...(memo && { isCompleted }),
    };

    try {
      if (memo) {
        // Update existing memo
        await memoService.updateMemo(memo.id, requestData as UpdateMemoRequest);
        memoStore.updateMemoInList(memo.id, requestData); // Update store directly
      } else {
        // Create new memo
        await memoService.createMemo(requestData as CreateMemoRequest);

        tryFetchNextMemo();
      }
      onClose(); // Call onClose on success
    } catch {
      const errorMsg = memo
        ? 'Failed to update memo. Please try again.'
        : 'Failed to create memo. Please try again.';
      memoStore.setError(errorMsg); // Set error in the store
      // Do not call onClose() on error, let the user see the error and retry/cancel
    } finally {
      isSaving = false;
      memoStore.setItemLoadingStatus(loadingId, false);
    }
  }

  function handleCancel() {
    if (isSaving) return; // Don't allow cancel while saving
    onClose();
  }
</script>

{#if show}
  <div class="fixed inset-0 z-10 overflow-y-auto">
    <div
      class="flex min-h-screen items-end justify-center px-4 pb-64 pt-4 text-center sm:block sm:p-0"
    >
      <!-- Background overlay -->
      <div class="fixed inset-0 transition-opacity" aria-hidden="true">
        <div class="absolute inset-0 bg-gray-500 opacity-75"></div>
      </div>

      <!-- Modal panel -->
      <div
        class="inline-block transform overflow-hidden rounded-lg bg-white text-left align-bottom shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:align-middle"
      >
        <form onsubmit={handleSubmit}>
          <div class="bg-white px-4 pb-4 pt-5 sm:p-6">
            <div class="sm:flex sm:items-start">
              <div class="mt-3 w-full text-center sm:mt-0 sm:text-left">
                <h3 class="text-lg font-medium leading-6 text-gray-900">
                  {memo ? 'Edit Memo' : 'Create New Memo'}
                </h3>

                <div class="mt-4 space-y-4">
                  <!-- Title -->
                  <div>
                    <label
                      for="title"
                      class="block text-sm font-medium text-gray-700"
                    >
                      Title
                    </label>
                    <input
                      type="text"
                      maxLength={100}
                      id="title"
                      bind:value={title}
                      disabled={isSaving}
                      placeholder="Enter title"
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500 disabled:bg-gray-100 sm:text-sm"
                    />
                  </div>

                  <!-- Content -->
                  <div>
                    <label
                      for="content"
                      class="block text-sm font-medium text-gray-700"
                    >
                      Content
                    </label>
                    <textarea
                      id="content"
                      bind:value={content}
                      maxLength={1000}
                      disabled={isSaving}
                      placeholder="Enter content"
                      rows="3"
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500 disabled:bg-gray-100 sm:text-sm"
                    ></textarea>
                  </div>

                  <!-- Reminder Time -->
                  <div>
                    <label
                      for="reminderTime"
                      class="block text-sm font-medium text-gray-700"
                    >
                      Reminder Time (Optional)
                    </label>
                    <input
                      type="datetime-local"
                      id="reminderTime"
                      bind:value={reminderTime}
                      disabled={isSaving}
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500 disabled:bg-gray-100 sm:text-sm"
                    />
                  </div>

                  <!-- Is Completed (only for edit mode) -->
                  {#if memo}
                    <div class="flex items-center">
                      <input
                        type="checkbox"
                        id="isCompleted"
                        bind:checked={isCompleted}
                        disabled={isSaving}
                        class="h-4 w-4 rounded border-gray-300 text-purple-600 focus:ring-purple-500 disabled:cursor-not-allowed"
                      />
                      <label
                        for="isCompleted"
                        class="ml-2 block text-sm text-gray-900 {isSaving
                          ? 'cursor-not-allowed text-gray-500'
                          : ''}"
                      >
                        Mark as completed
                      </label>
                    </div>
                  {/if}
                </div>
              </div>
            </div>
          </div>

          <div class="bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6">
            <button
              type="submit"
              disabled={isSaving}
              class="inline-flex w-full justify-center rounded-md border border-transparent bg-purple-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 sm:ml-3 sm:w-auto sm:text-sm"
            >
              {#if isSaving}
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
                Saving...
              {:else}
                {memo ? 'Save Changes' : 'Create Memo'}
              {/if}
            </button>
            <button
              type="button"
              onclick={handleCancel}
              disabled={isSaving}
              class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 sm:mt-0 sm:w-auto sm:text-sm"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
{/if}
