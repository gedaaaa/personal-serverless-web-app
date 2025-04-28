import { ApiError } from './error';

const API_BASE_URL = import.meta.env.VITE_PUBLIC_API_BASE_URL || '/api';

import { getAuthToken } from '$lib/auth';

let _client: ApiClient | null = null;
export const getDefaultClient = () => {
  if (!_client) {
    _client = new ApiClient();
  }
  return _client;
};

export class ApiClient {
  private readonly baseUrl: string;
  private authToken: string | null = null;

  constructor(baseUrl = API_BASE_URL) {
    this.baseUrl = baseUrl;
    // Try to get token using auth helper
    this.authToken = getAuthToken();
  }

  setAuthToken(token: string | null): void {
    this.authToken = token;
  }

  async request<T>(url: string, options: RequestInit = {}): Promise<T> {
    try {
      const headers = new Headers({
        'Content-Type': 'application/json',
      });

      if (this.authToken) {
        headers.set('Authorization', `Bearer ${this.authToken}`);
      }

      // Merge provided headers with default headers
      const requestOptions: RequestInit = {
        ...options,
        headers,
        credentials: 'include',
      };

      const response = await fetch(`${this.baseUrl}${url}`, requestOptions);

      if (!response.ok) {
        const errorMessage = response.statusText;
        let errorData: unknown;
        try {
          errorData = await response.json();
        } catch {
          // if JSON parsing fails, return empty object
          errorData = {};
        }

        throw new ApiError(response.status, errorMessage, errorData);
      }

      // check if content length is 0 or status is 204
      const contentLength = response.headers?.get('content-length');
      if (contentLength === '0' || response.status === 204) {
        return {} as T;
      }

      try {
        return (await response.json()) as T;
      } catch {
        // if JSON parsing fails, return empty object
        console.warn('Failed to parse JSON response');
        return {} as T;
      }
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  get<T>(endpoint: string) {
    return this.request<T>(endpoint, {
      method: 'GET',
    });
  }

  post<T>(endpoint: string, body: unknown) {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  }

  put<T>(endpoint: string, body: unknown) {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
  }

  delete<T>(endpoint: string) {
    return this.request<T>(endpoint, {
      method: 'DELETE',
    });
  }
}
