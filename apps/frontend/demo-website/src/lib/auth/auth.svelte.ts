import { browser } from '$app/environment';
import { goto } from '$app/navigation';

// Define auth types
export interface User {
  id: string;
  username: string;
}

export interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
}

// Initialize auth state from localStorage if available
const initialState: AuthState = {
  token: null,
  user: null,
  isAuthenticated: false,
};

// Load state from localStorage if in browser
if (browser) {
  const storedToken = localStorage.getItem('auth_token');
  const storedUser = localStorage.getItem('auth_user');

  if (storedToken && storedUser) {
    try {
      initialState.token = storedToken;
      initialState.user = JSON.parse(storedUser);
      initialState.isAuthenticated = true;
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (_) {
      // Invalid stored data, clear it
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_user');
    }
  }
}

// Create the auth store
export const auth = $state(initialState);

/**
 * Login user with token and username
 * @param token JWT token
 * @param username Username
 */
export const login = (token: string, username: string): void => {
  const user: User = {
    id: username, // Using username as ID for simplicity
    username,
  };

  // Update store
  auth.token = token;
  auth.user = user;
  auth.isAuthenticated = true;

  // Save to localStorage
  if (browser) {
    localStorage.setItem('auth_token', token);
    localStorage.setItem('auth_user', JSON.stringify(user));
  }
};

/**
 * Logout current user
 * @param redirectTo Optional path to redirect after logout
 */
export const logout = (redirectTo: string | null = null): void => {
  // Update store
  auth.token = null;
  auth.user = null;
  auth.isAuthenticated = false;

  // Clear localStorage
  if (browser) {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
  }

  // Redirect to specified page
  if (redirectTo) {
    goto(redirectTo);
  }
};

/**
 * Check if user is authenticated
 * @returns True if user is authenticated, false otherwise
 */
export const isAuthenticated = (): boolean => {
  if (browser) {
    const token = localStorage.getItem('auth_token');
    return !!token;
  }
  return false;
};

/**
 * Get current auth token
 * @returns Auth token or null if not authenticated
 */
export const getAuthToken = (): string | null => {
  if (browser) {
    return localStorage.getItem('auth_token');
  }
  return null;
};

/**
 * Get current user
 * @returns User object or null if not authenticated
 */
export const getCurrentUser = (): User | null => {
  if (browser) {
    const userJson = localStorage.getItem('auth_user');
    if (userJson) {
      try {
        return JSON.parse(userJson);
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
      } catch (_) {
        return null;
      }
    }
  }
  return null;
};
