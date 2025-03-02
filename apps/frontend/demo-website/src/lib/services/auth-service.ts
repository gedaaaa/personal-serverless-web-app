import { ApiClient } from '$lib/api';
import { getDefaultClient } from '$lib/api/client';
import { login as authLogin } from '$lib/auth';

// Auth API path prefix
const AUTH_API_PREFIX = '/auth';

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
    const response = await this.apiClient.post<{ token: string }>(
      `${AUTH_API_PREFIX}/login`,
      request,
    );

    // Set token to API client
    this.apiClient.setAuthToken(response.token);

    // Update auth store
    authLogin(response.token, request.username);

    return response.token;
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
}
