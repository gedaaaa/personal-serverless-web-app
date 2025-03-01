import {
  describe,
  it,
  expect,
  vi,
  beforeEach,
  afterEach,
  beforeAll,
} from 'vitest';
import { get } from 'svelte/store';
import { goto } from '$app/navigation';
import { browser } from '$app/environment';

// Mock dependencies BEFORE importing auth module
vi.mock('$app/environment', () => ({
  browser: true,
}));

vi.mock('$app/navigation', () => ({
  goto: vi.fn(),
}));

// Create localStorage mock
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
  length: 0,
  key: vi.fn(),
};

// Use vi.stubGlobal to mock localStorage
vi.stubGlobal('localStorage', localStorageMock);

// Import auth module and dependencies AFTER setting up all mocks
import * as auth from './index';

describe('Auth Module', () => {
  let store: Record<string, string> = {};

  beforeAll(() => {
    // Verify that browser is true
    expect(browser).toBe(true);

    // Setup localStorage mock implementations
    vi.mocked(localStorageMock.getItem).mockImplementation(
      (key: string) => store[key] || null,
    );

    vi.mocked(localStorageMock.setItem).mockImplementation(
      (key: string, value: string) => {
        store[key] = value;
      },
    );

    vi.mocked(localStorageMock.removeItem).mockImplementation((key: string) => {
      delete store[key];
    });

    vi.mocked(localStorageMock.clear).mockImplementation(() => {
      store = {};
    });
  });

  beforeEach(() => {
    // Reset mocks and localStorage
    vi.clearAllMocks();
    store = {};

    // Reset auth store
    auth.auth.set({
      token: null,
      user: null,
      isAuthenticated: false,
    });
  });

  afterEach(() => {
    // Reset auth store after each test
    auth.auth.set({
      token: null,
      user: null,
      isAuthenticated: false,
    });
  });

  describe('login', () => {
    it('should update auth store', () => {
      // Call login
      auth.login('mock-token', 'testuser');

      // Check if auth store was updated
      const state = get(auth.auth);
      expect(state.token).toBe('mock-token');
      expect(state.user).toEqual({ id: 'testuser', username: 'testuser' });
      expect(state.isAuthenticated).toBe(true);
    });

    it('should save to localStorage', () => {
      // Call login
      auth.login('mock-token', 'testuser');

      // Check if localStorage was updated
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'auth_token',
        'mock-token',
      );
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'auth_user',
        JSON.stringify({ id: 'testuser', username: 'testuser' }),
      );
    });
  });

  describe('logout', () => {
    it('should update auth store', () => {
      // Setup initial state
      auth.login('mock-token', 'testuser');

      // Call logout
      auth.logout();

      // Check if auth store was updated
      const state = get(auth.auth);
      expect(state.token).toBeNull();
      expect(state.user).toBeNull();
      expect(state.isAuthenticated).toBe(false);
    });

    it('should clear localStorage', () => {
      // Setup initial state
      auth.login('mock-token', 'testuser');

      // Call logout
      auth.logout();

      // Check if localStorage was cleared
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_token');
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_user');
    });

    it('should redirect to specified path', () => {
      // Call logout with custom redirect path
      auth.logout('/custom-path');

      // Check if redirect occurred
      expect(goto).toHaveBeenCalledWith('/custom-path');
    });

    it('should redirect to home page by default', () => {
      // Call logout without redirect path
      auth.logout();

      // Check if redirect occurred
      expect(goto).toHaveBeenCalledWith('/');
    });
  });

  describe('isAuthenticated', () => {
    it('should return true if token exists', () => {
      // Setup localStorage
      localStorageMock.setItem('auth_token', 'mock-token');

      // Check if isAuthenticated returns true
      expect(auth.isAuthenticated()).toBe(true);
    });

    it('should return false if token does not exist', () => {
      // Check if isAuthenticated returns false
      expect(auth.isAuthenticated()).toBe(false);
    });
  });

  describe('getAuthToken', () => {
    it('should return token from localStorage', () => {
      // Setup localStorage
      localStorageMock.setItem('auth_token', 'mock-token');

      // Check if getAuthToken returns token
      expect(auth.getAuthToken()).toBe('mock-token');
    });

    it('should return null if token does not exist', () => {
      // Check if getAuthToken returns null
      expect(auth.getAuthToken()).toBeNull();
    });
  });

  describe('getCurrentUser', () => {
    it('should return user from localStorage', () => {
      // Setup localStorage
      const user = { id: 'testuser', username: 'testuser' };
      localStorageMock.setItem('auth_user', JSON.stringify(user));

      // Check if getCurrentUser returns user
      expect(auth.getCurrentUser()).toEqual(user);
    });

    it('should return null if user does not exist', () => {
      // Check if getCurrentUser returns null
      expect(auth.getCurrentUser()).toBeNull();
    });

    it('should return null if user JSON is invalid', () => {
      // Setup localStorage with invalid JSON
      localStorageMock.setItem('auth_user', 'invalid-json');

      // Check if getCurrentUser returns null
      expect(auth.getCurrentUser()).toBeNull();
    });
  });
});
