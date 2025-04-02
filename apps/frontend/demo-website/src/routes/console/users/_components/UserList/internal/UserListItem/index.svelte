<script lang="ts">
  import type {
    User,
    UpdateUserRequest,
  } from '../../../../_services/userService';
  import EditModal from './internal/EditModal.svelte';
  import DeleteModal from './internal/DeleteModal.svelte';

  // Component props
  const { user, isLoading, onUpdate, onDelete } = $props<{
    user: User;
    isLoading: boolean;
    onUpdate: (userId: string, data: UpdateUserRequest) => void;
    onDelete: (userId: string) => void;
  }>();

  let showEditModal = $state(false);
  let showDeleteModal = $state(false);

  function handleUpdate(data: UpdateUserRequest) {
    onUpdate(user.id, data);
    showEditModal = false;
  }

  function handleDelete() {
    onDelete(user.id);
    showDeleteModal = false;
  }
</script>

<tr class:opacity-50={isLoading}>
  <td class="whitespace-nowrap px-6 py-4 text-sm font-medium text-gray-900">
    {user.username}
  </td>
  <td class="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
    {user.email}
  </td>
  <td class="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
    {user.fullName || '-'}
  </td>
  <td class="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
    {#if user.roles.includes('ROLE_ADMIN')}
      <span
        class="inline-flex rounded-full bg-purple-100 px-2 text-xs font-semibold leading-5 text-purple-800"
      >
        Admin
      </span>
    {:else}
      <span
        class="inline-flex rounded-full bg-gray-100 px-2 text-xs font-semibold leading-5 text-gray-800"
      >
        User
      </span>
    {/if}
  </td>
  <td class="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
    <button
      onclick={() => (showEditModal = true)}
      class="mr-2 text-purple-600 hover:text-purple-900"
      disabled={isLoading}
    >
      Edit
    </button>
    <button
      onclick={() => (showDeleteModal = true)}
      class="text-red-600 hover:text-red-900"
      disabled={isLoading}
    >
      Delete
    </button>
  </td>
</tr>

<EditModal
  {user}
  show={showEditModal}
  onUpdate={handleUpdate}
  onClose={() => (showEditModal = false)}
/>

<DeleteModal
  {user}
  show={showDeleteModal}
  onDelete={handleDelete}
  onClose={() => (showDeleteModal = false)}
/>
