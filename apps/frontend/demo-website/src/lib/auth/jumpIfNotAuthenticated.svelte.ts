import { auth } from './auth.svelte';
import { goto } from '$app/navigation';
import { page } from '$app/state';

export function jumpToLoginIfNotAuthenticated() {
  $effect(() => {
    const currentUrl = $derived(
      page.url?.pathname || '' + page.url?.search || '' + page.url?.hash || '',
    );
    if (!auth.isAuthenticated) {
      goto('/login?location=' + encodeURIComponent(currentUrl));
    }
  });
}
