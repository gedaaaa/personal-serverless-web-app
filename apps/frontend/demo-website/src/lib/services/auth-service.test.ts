import { describe, it, expect, vi, beforeEach } from 'vitest';
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
  let authService: AuthService;
  let mockApiClient: {
    post: ReturnType<typeof vi.fn>;
    setAuthToken: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks();

    // Setup mock API client
    mockApiClient = {
      post: vi.fn(),
      setAuthToken: vi.fn(),
    };

    // Cast the mock to ApiClient type
    (ApiClient as unknown as ReturnType<typeof vi.fn>).mockImplementation(
      () => mockApiClient,
    );

    // Create AuthService instance with mocked dependencies
    authService = new AuthService(mockApiClient as unknown as ApiClient);
  });

  describe('login', () => {
    it('should call API client with correct parameters', async () => {
      // Setup
      const loginRequest: LoginRequest = {
        username: 'testuser',
        password: 'password123',
      };
      const mockResponse = { token: 'mock-token' };
      mockApiClient.post.mockResolvedValue(mockResponse);

      // Execute
      const result = await authService.login(loginRequest);

      // Verify
      expect(mockApiClient.post).toHaveBeenCalledWith(
        '/auth/login',
        loginRequest,
      );
      expect(result).toBe('mock-token');
    });

    it('should set auth token and update auth store', async () => {
      // Setup
      const loginRequest: LoginRequest = {
        username: 'testuser',
        password: 'password123',
      };
      const mockResponse = { token: 'mock-token' };
      mockApiClient.post.mockResolvedValue(mockResponse);

      // Execute
      await authService.login(loginRequest);

      // Verify
      expect(mockApiClient.setAuthToken).toHaveBeenCalledWith('mock-token');
      expect(authModule.login).toHaveBeenCalledWith('mock-token', 'testuser');
    });

    it('should throw error when API call fails', async () => {
      // Setup
      const loginRequest: LoginRequest = {
        username: 'testuser',
        password: 'password123',
      };
      const mockError = new Error('API error');
      mockApiClient.post.mockRejectedValue(mockError);

      // Execute & Verify
      await expect(authService.login(loginRequest)).rejects.toThrow(
        'API error',
      );
    });
  });

  describe('register', () => {
    it('should call API client with correct parameters', async () => {
      // Setup
      const registerRequest: RegisterRequest = {
        username: 'testuser',
        email: 'test@example.com',
        password: 'password123',
        fullName: 'Test User',
      };
      const mockResponse = { id: 'user-123' };
      mockApiClient.post.mockResolvedValue(mockResponse);

      // Execute
      const result = await authService.register(registerRequest);

      // Verify
      expect(mockApiClient.post).toHaveBeenCalledWith(
        '/auth/register',
        registerRequest,
      );
      expect(result).toBe('user-123');
    });

    it('should throw error when API call fails', async () => {
      // Setup
      const registerRequest: RegisterRequest = {
        username: 'testuser',
        email: 'test@example.com',
        password: 'password123',
      };
      const mockError = new Error('API error');
      mockApiClient.post.mockRejectedValue(mockError);

      // Execute & Verify
      await expect(authService.register(registerRequest)).rejects.toThrow(
        'API error',
      );
    });
  });
});
