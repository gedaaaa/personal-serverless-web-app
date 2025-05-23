import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthService } from './auth-service';
import type { LoginRequest, RegisterRequest } from './auth-service';
import { getDefaultClient } from '$lib/api/client';
import * as authModule from '$lib/auth';
import { sha256 } from '$lib/utils/sha256';
import { ApiError } from '$lib/api/error';
// Mock dependencies
vi.mock('$lib/api/client', () => ({
  getDefaultClient: vi.fn(),
}));

vi.mock('$lib/auth', () => ({
  login: vi.fn(),
}));

vi.mock('$lib/utils/sha256', () => ({
  sha256: vi.fn(),
}));

describe('AuthService', () => {
  // Test data
  const AUTH_API_PREFIX = '/auth/v1';
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
    vi.resetAllMocks();

    // Setup ApiClient mock
    (
      getDefaultClient as unknown as ReturnType<typeof vi.fn>
    ).mockImplementation(() => mockApiClient);

    (sha256 as unknown as ReturnType<typeof vi.fn>).mockImplementation(() =>
      Promise.resolve('hashed-password'),
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
        {
          username: loginRequest.username,
          password: await sha256(loginRequest.password),
        },
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

    it('should handle password migration when 403 with migrationToken is received', async () => {
      // Setup mock migration error
      const migrationToken = 'migration-token-123';

      const migrationError = new ApiError(403, 'Password migration required', {
        migrationToken,
      });

      // Setup login to fail with migration error first time
      mockApiClient.post
        .mockImplementationOnce(() => {
          throw migrationError;
        })
        .mockImplementationOnce(() => {
          return { token: mockToken };
        });

      // Call login method
      const result = await authService.login(loginRequest);

      // Verify API client was called with hashed password, which leads to migration error
      expect(mockApiClient.post).toHaveBeenNthCalledWith(
        1,
        `${AUTH_API_PREFIX}/login`,
        {
          username: loginRequest.username,
          password: 'hashed-password',
        },
      );
      expect(mockApiClient.post).toHaveBeenNthCalledWith(
        2,
        `${AUTH_API_PREFIX}/migrate-password`,
        {
          username: loginRequest.username,
          migrationToken,
          originalPassword: loginRequest.password,
          desiredPassword: 'hashed-password',
        },
      );

      // Verify auth was updated
      expect(authModule.login).toHaveBeenCalledWith(
        mockToken,
        loginRequest.username,
      );

      // Verify result
      expect(result).toBe(mockToken);
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

      (sha256 as unknown as ReturnType<typeof vi.fn>).mockImplementation(() =>
        Promise.resolve('hashed-password'),
      );

      // Call register method
      await authService.register(registerRequest);

      // Verify API client was called correctly
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/register`,
        { ...registerRequest, password: 'hashed-password' },
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

  describe('verifyEmail', () => {
    const verificationToken = 'test-verification-token';
    const successMessage = 'Email verified successfully';

    it('should call API client with correct endpoint and token', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ message: successMessage });

      // Call verifyEmail method
      await authService.verifyEmail(verificationToken);

      // Verify API client was called correctly
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/email-verification/token`,
        { token: verificationToken },
      );
    });

    it('should return success message from response', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ message: successMessage });

      // Call verifyEmail method
      const result = await authService.verifyEmail(verificationToken);

      // Verify result
      expect(result).toBe(successMessage);
    });

    it('should throw error if API request fails', async () => {
      // Setup mock to throw error
      const error = new Error('Invalid verification token');
      mockApiClient.post.mockRejectedValue(error);

      // Call verifyEmail method and expect it to throw
      await expect(authService.verifyEmail(verificationToken)).rejects.toBe(
        error,
      );
    });
  });

  describe('resendVerificationEmail', () => {
    const email = 'user@example.com';
    const successMessage = 'Verification email sent successfully';

    it('should call API client with correct endpoint and email', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ message: successMessage });

      // Call resendVerificationEmail method
      await authService.resendVerificationEmail(email);

      // Verify API client was called correctly
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/email-verification/resend-email`,
        { email },
      );
    });

    it('should return success message from response', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ message: successMessage });

      // Call resendVerificationEmail method
      const result = await authService.resendVerificationEmail(email);

      // Verify result
      expect(result).toBe(successMessage);
    });

    it('should throw error if API request fails', async () => {
      // Setup mock to throw error
      const error = new Error('Email not found');
      mockApiClient.post.mockRejectedValue(error);

      // Call resendVerificationEmail method and expect it to throw
      await expect(authService.resendVerificationEmail(email)).rejects.toBe(
        error,
      );
    });
  });

  describe('migratePassword', () => {
    const username = 'testuser';
    const migrationToken = 'migration-token';
    const originalPassword = 'old-password';
    const desiredPassword = 'hashed-new-password';

    it('should call API client with correct endpoint and data', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ token: mockToken });

      // Call migratePassword method
      await authService.migratePassword(
        username,
        migrationToken,
        originalPassword,
        desiredPassword,
      );

      // Verify API client was called correctly
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `${AUTH_API_PREFIX}/migrate-password`,
        {
          username,
          migrationToken,
          originalPassword,
          desiredPassword,
        },
      );
    });

    it('should return token from response', async () => {
      // Setup mock response
      mockApiClient.post.mockResolvedValue({ token: mockToken });

      // Call migratePassword method
      const result = await authService.migratePassword(
        username,
        migrationToken,
        originalPassword,
        desiredPassword,
      );

      // Verify result
      expect(result).toBe(mockToken);
    });
  });
});
