import { browser } from '$app/environment';
import { redirect } from '@sveltejs/kit';
import { isAuthenticated } from '$lib/auth';

export const load = () => {
  if (browser) {
    // Check if user is authenticated, if not redirect to login page
    if (!isAuthenticated()) {
      throw redirect(302, '/');
    }
  }

  return {};
};
