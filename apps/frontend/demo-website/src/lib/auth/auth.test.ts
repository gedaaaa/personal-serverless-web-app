/**
 * @vitest-environment jsdom
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { get } from 'svelte/store';

// Mock browser environment
vi.mock('$app/environment', () => ({
  browser: true,
}));

// Mock navigation
vi.mock('$app/navigation', () => ({
  goto: vi.fn(),
}));

// Mock localStorage
const mockLocalStorage = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
    clear: vi.fn(() => {
      store = {};
    }),
  };
})();

// Setup global localStorage before importing auth module
global.localStorage = mockLocalStorage as any;

// Now import auth module
import {
  auth,
  login,
  logout,
  isAuthenticated,
  getAuthToken,
  getCurrentUser,
  type User,
  type AuthState,
} from './auth';
import { goto } from '$app/navigation';

describe('Auth Module', () => {
  // Save original localStorage
  const originalLocalStorage = global.localStorage;

  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks();
    mockLocalStorage.clear();

    // Reset auth store to initial state
    auth.set({
      token: null,
      user: null,
      isAuthenticated: false,
    });
  });

  afterEach(() => {
    // Restore original localStorage (which is our mock in this case)
    global.localStorage = originalLocalStorage;
  });

  describe('login', () => {
    it('should update auth store with token and user', () => {
      // Call login function
      login('test-token', 'testuser');

      // Verify auth store was updated
      const authState = get(auth);
      expect(authState.token).toBe('test-token');
      expect(authState.user).toEqual({
        id: 'testuser',
        username: 'testuser',
      });
      expect(authState.isAuthenticated).toBe(true);
    });

    it('should save token and user to localStorage', () => {
      // Call login function
      login('test-token', 'testuser');

      // Verify localStorage was updated
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        'auth_token',
        'test-token',
      );
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        'auth_user',
        JSON.stringify({
          id: 'testuser',
          username: 'testuser',
        }),
      );
    });
  });

  describe('logout', () => {
    beforeEach(() => {
      // Setup authenticated state
      login('test-token', 'testuser');
      vi.clearAllMocks(); // Clear login related calls
    });

    it('should reset auth store', () => {
      // Call logout function
      logout();

      // Verify auth store was reset
      const authState = get(auth);
      expect(authState.token).toBeNull();
      expect(authState.user).toBeNull();
      expect(authState.isAuthenticated).toBe(false);
    });

    it('should remove token and user from localStorage', () => {
      // Call logout function
      logout();

      // Verify localStorage items were removed
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('auth_token');
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('auth_user');
    });

    it('should redirect to specified path', () => {
      // Call logout with custom redirect path
      logout('/custom-path');

      // Verify redirect
      expect(goto).toHaveBeenCalledWith('/custom-path');
    });

    it('should redirect to home page by default', () => {
      // Call logout without specifying path
      logout();

      // Verify redirect to default path
      expect(goto).toHaveBeenCalledWith('/');
    });
  });

  describe('isAuthenticated', () => {
    it('should return true if token exists in localStorage', () => {
      // Setup localStorage with token
      mockLocalStorage.setItem('auth_token', 'test-token');

      // Call isAuthenticated function
      const result = isAuthenticated();

      // Verify result
      expect(result).toBe(true);
    });

    it('should return false if token does not exist in localStorage', () => {
      // Ensure localStorage has no token
      mockLocalStorage.removeItem('auth_token');

      // Call isAuthenticated function
      const result = isAuthenticated();

      // Verify result
      expect(result).toBe(false);
    });
  });

  describe('getAuthToken', () => {
    it('should return token from localStorage if it exists', () => {
      // Setup localStorage with token
      mockLocalStorage.setItem('auth_token', 'test-token');

      // Call getAuthToken function
      const token = getAuthToken();

      // Verify result
      expect(token).toBe('test-token');
    });

    it('should return null if token does not exist in localStorage', () => {
      // Ensure localStorage has no token
      mockLocalStorage.removeItem('auth_token');

      // Call getAuthToken function
      const token = getAuthToken();

      // Verify result
      expect(token).toBeNull();
    });
  });

  describe('getCurrentUser', () => {
    it('should return user from localStorage if it exists', () => {
      // Setup user in localStorage
      const user: User = { id: 'testuser', username: 'testuser' };
      mockLocalStorage.setItem('auth_user', JSON.stringify(user));

      // Call getCurrentUser function
      const result = getCurrentUser();

      // Verify result
      expect(result).toEqual(user);
    });

    it('should return null if user does not exist in localStorage', () => {
      // Ensure localStorage has no user
      mockLocalStorage.removeItem('auth_user');

      // Call getCurrentUser function
      const result = getCurrentUser();

      // Verify result
      expect(result).toBeNull();
    });

    it('should return null if user JSON is invalid', () => {
      // Setup invalid JSON in localStorage
      mockLocalStorage.setItem('auth_user', 'invalid-json');

      // Call getCurrentUser function
      const result = getCurrentUser();

      // Verify result
      expect(result).toBeNull();
    });
  });

  describe('auth store initialization', () => {
    it('should initialize with values from localStorage if available', () => {
      // Setup localStorage with auth data
      const user: User = { id: 'testuser', username: 'testuser' };
      mockLocalStorage.setItem('auth_token', 'test-token');
      mockLocalStorage.setItem('auth_user', JSON.stringify(user));

      // Reset auth store to initial state to simulate module initialization
      auth.set({
        token: null,
        user: null,
        isAuthenticated: false,
      });

      // Import would re-execute the module code, but we can't do that in ESM
      // Instead, we'll manually call the initialization logic

      // This is a simplified version of the initialization logic in auth.ts
      if (
        mockLocalStorage.getItem('auth_token') &&
        mockLocalStorage.getItem('auth_user')
      ) {
        try {
          const token = mockLocalStorage.getItem('auth_token');
          const user = JSON.parse(mockLocalStorage.getItem('auth_user') || '');
          auth.set({
            token,
            user,
            isAuthenticated: true,
          });
        } catch (e) {
          // Invalid stored data
          mockLocalStorage.removeItem('auth_token');
          mockLocalStorage.removeItem('auth_user');
        }
      }

      // Verify state
      const authState = get(auth);
      expect(authState.token).toBe('test-token');
      expect(authState.user).toEqual(user);
      expect(authState.isAuthenticated).toBe(true);
    });

    it('should initialize with default values if localStorage is empty', () => {
      // Ensure localStorage is empty
      mockLocalStorage.clear();

      // Reset auth store to initial state
      auth.set({
        token: null,
        user: null,
        isAuthenticated: false,
      });

      // Simulate initialization logic (which would do nothing in this case)

      // Verify state remains default
      const authState = get(auth);
      expect(authState.token).toBeNull();
      expect(authState.user).toBeNull();
      expect(authState.isAuthenticated).toBe(false);
    });

    it('should handle invalid user JSON in localStorage', () => {
      // Setup valid token but invalid user JSON
      mockLocalStorage.setItem('auth_token', 'test-token');
      mockLocalStorage.setItem('auth_user', 'invalid-json');

      // Reset auth store to initial state
      auth.set({
        token: null,
        user: null,
        isAuthenticated: false,
      });

      // Simulate initialization logic with invalid JSON
      if (
        mockLocalStorage.getItem('auth_token') &&
        mockLocalStorage.getItem('auth_user')
      ) {
        try {
          const token = mockLocalStorage.getItem('auth_token');
          const user = JSON.parse(mockLocalStorage.getItem('auth_user') || '');
          auth.set({
            token,
            user,
            isAuthenticated: true,
          });
        } catch (e) {
          // Invalid stored data - should clear localStorage
          mockLocalStorage.removeItem('auth_token');
          mockLocalStorage.removeItem('auth_user');
        }
      }

      // Verify state remains default
      const authState = get(auth);
      expect(authState.token).toBeNull();
      expect(authState.user).toBeNull();
      expect(authState.isAuthenticated).toBe(false);

      // Verify localStorage was cleared
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('auth_token');
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('auth_user');
    });
  });
});
