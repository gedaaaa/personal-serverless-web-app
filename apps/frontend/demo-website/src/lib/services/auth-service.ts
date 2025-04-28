import { ApiClient } from '$lib/api';
import { getDefaultClient } from '$lib/api/client';
import { login as authLogin } from '$lib/auth';
import { ApiError } from '$lib/api/error';
import { sha256 } from '$lib/utils/sha256';
// Auth API path prefix
const AUTH_API_PREFIX = '/auth/v1';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName?: string;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface VerifyEmailResponse {
  message: string;
}

export interface ResendVerificationEmailRequest {
  email: string;
}

export interface ResendVerificationEmailResponse {
  message: string;
}

/**
 * Service for handling authentication operations
 */
export class AuthService {
  private apiClient: ApiClient = getDefaultClient();

  /**
   * User login
   * @param request Login request
   * @returns JWT token
   */
  async login(request: LoginRequest): Promise<string> {
    const hashedPassword = await sha256(request.password);

    let token: string;
    try {
      const response = await this.apiClient.post<{ token: string }>(
        `${AUTH_API_PREFIX}/login`,
        {
          username: request.username,
          password: hashedPassword,
        },
      );

      // Set token to API client
      token = response.token;
    } catch (error) {
      // if the error is a 403 and the data contains a migrationToken, migrate the password
      if (
        error instanceof ApiError &&
        (error as ApiError<{ migrationToken: string }>).status === 403 &&
        (error as ApiError<{ migrationToken: string }>).data.migrationToken
      ) {
        const migrationToken = (error as ApiError<{ migrationToken: string }>)
          .data.migrationToken;
        const originalPassword = request.password;

        token = await this.migratePassword(
          request.username,
          migrationToken,
          originalPassword,
          hashedPassword,
        );
      } else {
        throw error;
      }
    }

    this.apiClient.setAuthToken(token);

    // Update auth store
    authLogin(token, request.username);
    return token;
  }

  /**
   * User registration
   * @param request Registration request
   * @returns User ID
   */
  async register(request: RegisterRequest): Promise<string> {
    const response = await this.apiClient.post<{ id: string }>(
      `${AUTH_API_PREFIX}/register`,
      request,
    );
    return response.id;
  }

  /**
   * Verify email with token
   * @param token Verification token
   * @returns Response message
   */
  async verifyEmail(token: string): Promise<string> {
    const response = await this.apiClient.post<VerifyEmailResponse>(
      `${AUTH_API_PREFIX}/email-verification/token`,
      { token },
    );
    return response.message;
  }

  /**
   * Resend verification email
   * @param email Email address
   * @returns Response message
   */
  async resendVerificationEmail(email: string): Promise<string> {
    const response = await this.apiClient.post<ResendVerificationEmailResponse>(
      `${AUTH_API_PREFIX}/email-verification/resend-email`,
      { email },
    );
    return response.message;
  }

  async migratePassword(
    username: string,
    migrationToken: string,
    originalPassword: string,
    desiredPassword: string,
  ): Promise<string> {
    const response = await this.apiClient.post<{ token: string }>(
      `${AUTH_API_PREFIX}/migrate-password`,
      { username, migrationToken, originalPassword, desiredPassword },
    );
    return response.token;
  }
}
