<script lang="ts">
  import type { Memo } from '../_services/memo-service';
  import { onMount } from 'svelte';

  // Component props
  const { memo, onToggleComplete, onEdit, onDelete, onConfirmDelete } = $props<{
    memo: Memo;
    onToggleComplete: (id: string, completed: boolean) => void;
    onEdit: (memo: Memo) => void;
    onDelete: (id: string) => void;
    onConfirmDelete: (memo: Memo) => void;
  }>();

  // 内容长度阈值，超过该长度将显示展开/折叠按钮
  const MAX_CONTENT_LENGTH = 100;

  // 用于控制内容展开/折叠状态
  let expanded = $state(false);
  let needsExpansion = $state(false);
  let contentElement: HTMLParagraphElement;

  // 检查内容是否需要展开功能
  onMount(() => {
    if (memo.content.length > MAX_CONTENT_LENGTH) {
      needsExpansion = true;
    }
  });

  // 切换展开/折叠状态
  function toggleExpand() {
    expanded = !expanded;
  }

  // Handle toggle complete status
  function handleToggleComplete() {
    onToggleComplete(memo.id, !memo.isCompleted);
  }

  // Format date for display
  function formatDate(dateString?: string): string {
    if (!dateString) return 'No reminder set';

    const date = new Date(dateString);
    return date.toLocaleString();
  }
</script>

<div
  class="mb-4 overflow-hidden rounded-lg bg-white shadow-sm transition hover:shadow-md"
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
               {memo.isCompleted ? 'bg-purple-600 ' : 'bg-gray-200'}"
        onclick={handleToggleComplete}
        aria-label={memo.isCompleted
          ? 'Mark as incomplete'
          : 'Mark as complete'}
      >
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
          class="mb-4 text-sm font-medium text-purple-600 hover:text-purple-700 focus:outline-none"
          onclick={toggleExpand}
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
      <span class="text-sm text-gray-500">
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
          class="text-gray-500 hover:text-purple-600"
          onclick={() => onEdit(memo)}
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
          class="text-gray-500 hover:text-red-600"
          onclick={() => onConfirmDelete(memo)}
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
