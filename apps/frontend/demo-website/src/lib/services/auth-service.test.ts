import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AuthService } from './auth-service';
import type { LoginRequest, RegisterRequest } from './auth-service';
import { ApiClient } from '$lib/api';
import * as authModule from '$lib/auth';

// Mock dependencies
vi.mock('$lib/api', () => ({
  ApiClient: vi.fn(),
}));

vi.mock('$lib/auth', () => ({
  login: vi.fn(),
}));

describe('AuthService', () => {
  // Test data
  const AUTH_API_PREFIX = '/auth';
  const mockToken = 'test-jwt-token';
  const mockUserId = 'user-123';

  // Mock API client
  const mockApiClient = {
    post: vi.fn(),
    setAuthToken: vi.fn(),
  };

  // Service instance
  let authService: AuthService;

  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks();

    // Setup ApiClient mock
    (ApiClient as unknown as ReturnType<typeof vi.fn>).mockImplementation(
      () => mockApiClient,
    );

    // Create service instance
    authService = new AuthService();
  });

  describe('login', () => {
    const loginRequest: LoginRequest = {
      username: 'testuser',
      password: 'password123',
    };

    it('should call API client with correct endpoint and request data', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ token: mockToken });

      // Call login method
      await authService.login(loginRequest);

      // Verify API client was called correctly
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/login`,
        loginRequest,
      );
    });

    it('should set auth token in API client', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ token: mockToken });

      // Call login method
      await authService.login(loginRequest);

      // Verify token was set in API client
      expect(mockApiClient.setAuthToken).toHaveBeenCalledWith(mockToken);
    });

    it('should update auth store with token and username', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ token: mockToken });

      // Call login method
      await authService.login(loginRequest);

      // Verify auth store was updated
      expect(authModule.login).toHaveBeenCalledWith(
        mockToken,
        loginRequest.username,
      );
    });

    it('should return token from response', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ token: mockToken });

      // Call login method
      const result = await authService.login(loginRequest);

      // Verify result
      expect(result).toBe(mockToken);
    });

    it('should throw error if API request fails', async () => {
      // Setup mock to throw error
      const error = new Error('Login failed');
      mockApiClient.post.mockRejectedValue(error);

      // Call login method and expect it to throw
      await expect(authService.login(loginRequest)).rejects.toBe(error);
    });
  });

  describe('register', () => {
    const registerRequest: RegisterRequest = {
      username: 'newuser',
      email: 'newuser@example.com',
      password: 'password123',
      fullName: 'New User',
    };

    it('should call API client with correct endpoint and request data', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ id: mockUserId });

      // Call register method
      await authService.register(registerRequest);

      // Verify API client was called correctly
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/register`,
        registerRequest,
      );
    });

    it('should return user ID from response', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ id: mockUserId });

      // Call register method
      const result = await authService.register(registerRequest);

      // Verify result
      expect(result).toBe(mockUserId);
    });

    it('should throw error if API request fails', async () => {
      // Setup mock to throw error
      const error = new Error('Registration failed');
      mockApiClient.post.mockRejectedValue(error);

      // Call register method and expect it to throw
      await expect(authService.register(registerRequest)).rejects.toBe(error);
    });
  });
});
