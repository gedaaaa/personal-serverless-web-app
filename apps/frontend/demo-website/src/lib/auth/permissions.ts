import { browser } from '$app/environment';
import { redirect } from '@sveltejs/kit';
import { getAuthToken } from './auth.svelte';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;
  roles: string[];
  exp: number;
  [key: string]: unknown;
}

/**
 * Check if the current user has the required role
 * @param requiredRole Role to check for
 * @returns True if user has the role, false otherwise
 */
export const hasRole = (requiredRole: string): boolean => {
  if (!browser) return false;

  const token = getAuthToken();
  if (!token) return false;

  try {
    const decoded = jwtDecode<JwtPayload>(token);

    // Check if token is expired
    const currentTime = Math.floor(Date.now() / 1000);
    if (decoded.exp < currentTime) return false;

    return decoded.roles.includes(requiredRole);
  } catch (error) {
    console.error('Error decoding JWT token:', error);
    return false;
  }
};

/**
 * Check if the current user is an admin
 * @returns True if user is an admin, false otherwise
 */
export const isAdmin = (): boolean => {
  return hasRole('ROLE_ADMIN');
};

/**
 * Protect a route, requiring the user to have a specific role
 * @param requiredRole Role required to access the route
 * @param redirectTo Path to redirect to if user doesn't have the required role
 */
export const requireRole = (requiredRole: string, redirectTo = '/'): void => {
  if (browser && !hasRole(requiredRole)) {
    throw redirect(302, redirectTo);
  }
};

/**
 * Protect a route, requiring the user to be an admin
 * @param redirectTo Path to redirect to if user is not an admin
 */
export const requireAdmin = (redirectTo = '/'): void => {
  requireRole('ROLE_ADMIN', redirectTo);
};
