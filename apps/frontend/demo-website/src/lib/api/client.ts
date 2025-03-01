const API_BASE_URL =
  import.meta.env.VITE_PUBLIC_API_BASE_URL || 'http://localhost:3000';

import { getAuthToken } from '$lib/auth';

interface ApiError {
  status: number;
  message: string;
  details?: unknown;
}

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

  async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;

    // Create new headers object
    const headers = new Headers(options.headers);
    headers.set('Content-Type', 'application/json');

    // Add auth token if available
    if (this.authToken) {
      headers.set('Authorization', `Bearer ${this.authToken}`);
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      });

      if (!response.ok) {
        const error: ApiError = {
          status: response.status,
          message: response.statusText,
        };
        throw error;
      }

      return (await response.json()) as Promise<T>;
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  get<T>(endpoint: string) {
    return this.request<T>(endpoint, { method: 'GET' });
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
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}
