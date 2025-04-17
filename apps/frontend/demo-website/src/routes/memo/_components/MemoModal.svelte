<script lang="ts">
  import type {
    Memo,
    CreateMemoRequest,
    UpdateMemoRequest,
  } from '../_services/memo-service';

  // Component props
  const {
    show = $bindable<boolean>(false),
    memo,
    onSave,
    onClose,
  } = $props<{
    show: boolean;
    memo?: Memo;
    onSave: (data: CreateMemoRequest | UpdateMemoRequest) => void;
    onClose: () => void;
  }>();

  // Form state
  let title = $state('');
  let content = $state('');
  let reminderTime = $state('');
  let isCompleted = $state(false);

  // Reset form when memo changes
  $effect(() => {
    if (memo) {
      title = memo.title;
      content = memo.content;
      isCompleted = memo.isCompleted;

      // Convert ISO string to local datetime-local format if it exists
      if (memo.reminderTime) {
        const date = new Date(memo.reminderTime);
        reminderTime = new Date(
          date.getTime() - date.getTimezoneOffset() * 60000,
        )
          .toISOString()
          .slice(0, 16);
      } else {
        reminderTime = '';
      }
    } else {
      title = '';
      content = '';
      reminderTime = '';
      isCompleted = false;
    }
  });

  // Handle form submission
  function handleSubmit(e: Event) {
    e.preventDefault();

    const data: CreateMemoRequest | UpdateMemoRequest = {
      title,
      content,
      reminderTime: reminderTime
        ? new Date(reminderTime).toISOString()
        : undefined,
      ...(memo ? { isCompleted } : {}),
    };

    onSave(data);
  }
</script>

{#if show}
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
                      id="title"
                      bind:value={title}
                      required
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500 sm:text-sm"
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
                      rows="3"
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500 sm:text-sm"
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
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500 sm:text-sm"
                    />
                  </div>

                  <!-- Is Completed (only for edit mode) -->
                  {#if memo}
                    <div class="flex items-center">
                      <input
                        type="checkbox"
                        id="isCompleted"
                        bind:checked={isCompleted}
                        class="h-4 w-4 rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                      />
                      <label
                        for="isCompleted"
                        class="ml-2 block text-sm text-gray-900"
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
              class="inline-flex w-full justify-center rounded-md border border-transparent bg-purple-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 sm:ml-3 sm:w-auto sm:text-sm"
            >
              {memo ? 'Save Changes' : 'Create Memo'}
            </button>
            <button
              type="button"
              onclick={onClose}
              class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 sm:mt-0 sm:w-auto sm:text-sm"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
{/if}
