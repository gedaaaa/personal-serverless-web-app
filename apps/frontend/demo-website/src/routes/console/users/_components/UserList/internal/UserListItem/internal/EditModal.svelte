<script lang="ts">
  import type {
    User,
    UpdateUserRequest,
  } from '../../../../../_services/userService';

  const { show, user, onUpdate, onClose } = $props<{
    show: boolean;
    user: User;
    onUpdate: (data: UpdateUserRequest) => void;
    onClose: () => void;
  }>();

  let editEmail = $state(user.email);
  let editPassword = $state('');
  let editFullName = $state(user.fullName || '');
  let isAdmin = $state(user.roles.includes('ROLE_ADMIN'));

  function handleSubmit() {
    const updateData: UpdateUserRequest = {
      email: editEmail !== user.email ? editEmail : undefined,
      fullName: editFullName !== user.fullName ? editFullName : undefined,
      roles: isAdmin ? ['ROLE_USER', 'ROLE_ADMIN'] : ['ROLE_USER'],
      password: editPassword || undefined,
    };

    if (Object.keys(updateData).length === 0) {
      onClose();
      return;
    }

    onUpdate(updateData);
    onClose();
  }
</script>

{#if show}
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
        <form
          onsubmit={(e) => {
            e.preventDefault();
            handleSubmit();
          }}
        >
          <div class="bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
            <div class="sm:flex sm:items-start">
              <div class="mt-3 w-full text-center sm:mt-0 sm:text-left">
                <h3 class="text-lg font-medium leading-6 text-gray-900">
                  Edit User
                </h3>
                <div class="mt-4 space-y-4">
                  <div>
                    <label
                      for="email"
                      class="block text-sm font-medium text-gray-700"
                    >
                      Email
                    </label>
                    <input
                      type="email"
                      id="email"
                      bind:value={editEmail}
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500 sm:text-sm"
                    />
                  </div>

                  <div>
                    <label
                      for="password"
                      class="block text-sm font-medium text-gray-700"
                    >
                      Password
                    </label>
                    <input
                      type="password"
                      id="password"
                      bind:value={editPassword}
                      placeholder="Leave empty to keep unchanged"
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500 sm:text-sm"
                    />
                  </div>

                  <div>
                    <label
                      for="fullName"
                      class="block text-sm font-medium text-gray-700"
                    >
                      Full Name
                    </label>
                    <input
                      type="text"
                      id="fullName"
                      bind:value={editFullName}
                      class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-purple-500 focus:outline-none focus:ring-purple-500 sm:text-sm"
                    />
                  </div>

                  <div>
                    <div class="flex items-center">
                      <input
                        type="checkbox"
                        id="isAdmin"
                        bind:checked={isAdmin}
                        class="h-4 w-4 rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                      />
                      <label
                        for="isAdmin"
                        class="ml-2 block text-sm text-gray-900"
                      >
                        Admin Privileges
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6">
            <button
              type="submit"
              class="inline-flex w-full justify-center rounded-md border border-transparent bg-purple-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 sm:ml-3 sm:w-auto sm:text-sm"
            >
              Save
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
