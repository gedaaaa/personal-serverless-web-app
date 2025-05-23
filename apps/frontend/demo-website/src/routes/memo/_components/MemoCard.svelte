<script lang="ts">
  import type { Memo } from '../_services/memo-service';
  import {
    setItemLoadingStatus,
    updateMemoInList,
    setError,
  } from '../_stores/memoStore.svelte';
  import memoService, {
    type UpdateMemoRequest,
  } from '../_services/memo-service';

  // Component props
  const {
    memo,
    loading = $bindable(false),
    onEdit,
    onDelete,
  } = $props<{
    memo: Memo;
    loading?: boolean;
    onEdit: (memo: Memo) => void;
    onDelete: (memo: Memo) => void;
  }>();

  /**
   * Handles the toggling of the complete status of a memo.
   */
  async function handleToggleComplete() {
    const id = memo.id;
    setItemLoadingStatus(id, true);
    try {
      const updateData: UpdateMemoRequest = {
        title: memo.title,
        content: memo.content,
        reminderTime: memo.reminderTime,
        isCompleted: !memo.isCompleted,
      };

      const updatedMemo = await memoService.updateMemo(id, updateData);

      // Update memo in the store list without full refetch
      updateMemoInList(id, updatedMemo);
    } catch {
      // Use the store's error handling
      setError('Failed to update memo status. Please try again.');
    } finally {
      setItemLoadingStatus(id, false);
    }
  }

  // Threshold for content length
  const MAX_CONTENT_LENGTH = 100;

  // For controlling content expansion/collapse state
  let expanded = $state(false);
  let needsExpansion = $state(false);
  let contentElement: HTMLParagraphElement;

  // Check if content needs expansion functionality
  $effect(() => {
    if (memo.content && memo.content.length > MAX_CONTENT_LENGTH) {
      needsExpansion = true;
    } else {
      needsExpansion = false;
      expanded = false;
    }
  });

  // Toggle expansion/collapse state
  function toggleExpand() {
    expanded = !expanded;
  }

  // Format date for display
  function formatDate(dateString?: string): string {
    if (!dateString) return 'No reminder set';

    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return 'Invalid reminder date';
      }
      return date.toLocaleString();
    } catch {
      return 'Error formatting date';
    }
  }

  function getDateClass(dateString?: string): string {
    if (!dateString) return 'text-gray-400';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return 'text-gray-400';
    }

    const now = new Date();
    if (date.getTime() <= now.getTime()) {
      return 'text-red-500';
    }

    if (date.getTime() <= now.getTime() + 1000 * 60 * 60) {
      return 'text-orange-500';
    }
    if (date.getTime() <= now.getTime() + 1000 * 60 * 60 * 24) {
      return 'text-amber-500';
    }

    return 'text-lime-500';
  }
</script>

<div
  class="mb-4 overflow-hidden rounded-lg bg-white shadow-sm transition hover:shadow-md {loading
    ? 'cursor-wait opacity-50'
    : ''}"
>
  <div class="relative p-5">
    <!-- Card Header -->
    <div class="mb-3 flex items-center justify-between">
      <h3
        class="text-lg font-medium text-gray-800 {memo.isCompleted
          ? 'text-gray-500 line-through'
          : ''}"
      >
        {memo.title}
      </h3>

      <!-- Toggle complete button -->
      <button
        class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-white transition-colors
               {memo.isCompleted ? 'bg-purple-600 ' : 'bg-gray-200'} {loading
          ? 'cursor-wait'
          : ''}"
        onclick={handleToggleComplete}
        disabled={loading}
        aria-label={memo.isCompleted
          ? 'Mark as incomplete'
          : 'Mark as complete'}
      >
        {#if !loading}
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-4 w-4"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
              clip-rule="evenodd"
            />
          </svg>
        {/if}
      </button>
    </div>

    <!-- Card Content -->
    <div>
      <p
        bind:this={contentElement}
        class="mb-2 text-wrap text-gray-600 {memo.isCompleted
          ? 'text-gray-400 line-through'
          : ''} {needsExpansion && !expanded
          ? 'line-clamp-3 overflow-hidden'
          : ''}"
      >
        {memo.content}
      </p>

      {#if needsExpansion}
        <button
          class="mb-4 text-sm font-medium text-purple-600 hover:text-purple-700 focus:outline-none disabled:cursor-not-allowed disabled:text-gray-400"
          onclick={toggleExpand}
          disabled={loading}
          aria-label={expanded ? 'Show less' : 'Show more'}
        >
          {expanded ? 'Show less' : 'Show more'}
        </button>
      {:else}
        <div class="mb-4"></div>
      {/if}
    </div>

    <!-- Card Footer -->
    <div class="flex items-center justify-between">
      <span
        class="inline-flex items-center text-sm {getDateClass(
          memo.reminderTime,
        )}"
      >
        {#if memo.reminderTime}
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="mr-1 inline h-4 w-4"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"
              clip-rule="evenodd"
            />
          </svg>
          {formatDate(memo.reminderTime)}
        {:else}
          No reminder
        {/if}
      </span>

      <div class="flex space-x-2">
        <!-- Edit button -->
        <button
          class="text-gray-500 hover:text-purple-600 disabled:cursor-not-allowed disabled:text-gray-400"
          onclick={() => onEdit(memo)}
          disabled={loading}
          aria-label="Edit memo"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"
            />
          </svg>
        </button>

        <!-- Delete button -->
        <button
          class="text-gray-500 hover:text-red-600 disabled:cursor-not-allowed disabled:text-gray-400"
          onclick={() => onDelete(memo)}
          disabled={loading}
          aria-label="Delete memo"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
              clip-rule="evenodd"
            />
          </svg>
        </button>
      </div>
    </div>
  </div>
</div>
