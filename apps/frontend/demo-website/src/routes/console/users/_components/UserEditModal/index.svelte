<script lang="ts">
  import { getContext } from 'svelte';
  import { USER_MANAGEMENT_KEY } from '../../_contexts/userManagement';
  import type { UserManagementContext } from '../../_contexts/userManagement';
  import EditForm from './internal/EditForm/index.svelte';

  // 获取上下文
  const { showEditModal, closeEditModal, saveUserEdit } =
    getContext<UserManagementContext>(USER_MANAGEMENT_KEY);
</script>

{#if $showEditModal}
  <div class="fixed inset-0 z-10 overflow-y-auto">
    <div
      class="flex min-h-screen items-end justify-center px-4 pb-20 pt-4 text-center sm:block sm:p-0"
    >
      <div class="fixed inset-0 transition-opacity" aria-hidden="true">
        <div class="absolute inset-0 bg-gray-500 opacity-75"></div>
      </div>

      <div
        class="inline-block transform overflow-hidden rounded-lg bg-white text-left align-bottom shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:align-middle"
      >
        <div class="bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
          <div class="sm:flex sm:items-start">
            <div class="mt-3 w-full text-center sm:mt-0 sm:text-left">
              <h3 class="text-lg font-medium leading-6 text-gray-900">
                编辑用户
              </h3>
              <EditForm
                on:save={(e) => saveUserEdit(e.detail)}
                on:cancel={closeEditModal}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
{/if}
