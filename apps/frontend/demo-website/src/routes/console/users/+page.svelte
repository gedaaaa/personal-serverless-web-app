<script lang="ts">
  import UserList from './_components/UserList';
  import { userListStore } from './_components/UserList/store';
</script>

<div>
  <div class="mb-6 flex items-center justify-between">
    <h1 class="text-2xl font-medium text-gray-800">User Management</h1>
  </div>

  {#if $userListStore.error}
    <div class="mb-4 rounded-md bg-red-50 p-4 text-red-600">
      {$userListStore.error}
    </div>
  {/if}

  <UserList />
</div>
