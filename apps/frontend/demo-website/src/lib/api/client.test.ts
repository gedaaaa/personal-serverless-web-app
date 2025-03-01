import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ApiClient } from './client';
import * as authModule from '$lib/auth/auth';

// Mock dependencies
vi.mock('$lib/auth', () => ({
  getAuthToken: vi.fn(),
}));

// Mock global fetch
const mockFetchResponse = {
  ok: true,
  json: vi.fn().mockResolvedValue({}),
};
const mockFetch = vi.fn().mockResolvedValue(mockFetchResponse);

// Save original fetch
const originalFetch = global.fetch;

describe('ApiClient', () => {
  const API_BASE_URL = 'http://test-api.com';
  let apiClient: ApiClient;

  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks();
    mockFetch.mockClear();
    mockFetchResponse.json.mockClear();

    // Replace global fetch with mock
    global.fetch = mockFetch;

    // Mock auth token
    (authModule.getAuthToken as ReturnType<typeof vi.fn>).mockReturnValue(null);

    // Create ApiClient instance
    apiClient = new ApiClient(API_BASE_URL);
  });

  afterEach(() => {
    // Restore original fetch
    global.fetch = originalFetch;
  });

  describe('constructor', () => {
    it('should use provided base URL', () => {
      const client = new ApiClient('http://custom-url.com');
      expect((client as any).baseUrl).toBe('http://custom-url.com');
    });

    it('should get auth token from auth module', () => {
      (authModule.getAuthToken as ReturnType<typeof vi.fn>).mockReturnValue(
        'existing-token',
      );
      const client = new ApiClient(API_BASE_URL);
      expect((client as any).authToken).toBe('existing-token');
    });
  });

  describe('setAuthToken', () => {
    it('should set auth token', () => {
      apiClient.setAuthToken('test-token');
      expect((apiClient as any).authToken).toBe('test-token');
    });

    it('should allow setting null token', () => {
      apiClient.setAuthToken('test-token');
      apiClient.setAuthToken(null);
      expect((apiClient as any).authToken).toBeNull();
    });
  });

  describe('request', () => {
    it('should call fetch with correct URL', async () => {
      // Setup mock response
      mockFetchResponse.json.mockResolvedValue({ data: 'test' });

      // Call request method
      await apiClient.request('/test-endpoint');

      // Verify fetch was called with correct URL
      expect(mockFetch).toHaveBeenCalledWith(
        `${API_BASE_URL}/test-endpoint`,
        expect.any(Object),
      );
    });

    it('should set Content-Type header', async () => {
      // Setup mock response
      mockFetchResponse.json.mockResolvedValue({ data: 'test' });

      // Call request method
      await apiClient.request('/test-endpoint');

      // Verify Content-Type header was set
      const options = mockFetch.mock.calls[0][1];
      expect(options.headers instanceof Headers).toBe(true);
      expect(options.headers.get('Content-Type')).toBe('application/json');
    });

    it('should add auth token to headers if available', async () => {
      // Setup auth token
      apiClient.setAuthToken('test-token');

      // Setup mock response
      mockFetchResponse.json.mockResolvedValue({ data: 'test' });

      // Call request method
      await apiClient.request('/test-endpoint');

      // Verify Authorization header was set
      const options = mockFetch.mock.calls[0][1];
      expect(options.headers.get('Authorization')).toBe('Bearer test-token');
    });

    it('should not add auth token to headers if not available', async () => {
      // Setup mock response
      mockFetchResponse.json.mockResolvedValue({ data: 'test' });

      // Call request method
      await apiClient.request('/test-endpoint');

      // Verify Authorization header was not set
      const options = mockFetch.mock.calls[0][1];
      expect(options.headers.has('Authorization')).toBe(false);
    });

    it('should return JSON response if request is successful', async () => {
      // Setup mock response
      const mockData = { data: 'test' };
      mockFetchResponse.json.mockResolvedValue(mockData);

      // Call request method
      const result = await apiClient.request('/test-endpoint');

      // Verify result
      expect(result).toEqual(mockData);
    });

    it('should throw error if request fails', async () => {
      // Setup mock response for failure
      const failResponse = {
        ok: false,
        status: 404,
        statusText: 'Not Found',
      };
      mockFetch.mockResolvedValueOnce(failResponse);

      // Call request method and expect it to throw
      await expect(apiClient.request('/test-endpoint')).rejects.toEqual({
        status: 404,
        message: 'Not Found',
      });
    });

    it('should throw error if fetch fails', async () => {
      // Setup fetch to fail
      const error = new Error('Network error');
      mockFetch.mockRejectedValueOnce(error);

      // Call request method and expect it to throw
      await expect(apiClient.request('/test-endpoint')).rejects.toBe(error);
    });
  });

  describe('HTTP methods', () => {
    beforeEach(() => {
      // Mock request method
      vi.spyOn(apiClient, 'request').mockResolvedValue({ data: 'test' });
    });

    it('should call request with GET method', async () => {
      await apiClient.get('/test');
      expect(apiClient.request).toHaveBeenCalledWith('/test', {
        method: 'GET',
      });
    });

    it('should call request with POST method and body', async () => {
      const body = { name: 'Test' };
      await apiClient.post('/test', body);
      expect(apiClient.request).toHaveBeenCalledWith('/test', {
        method: 'POST',
        body: JSON.stringify(body),
      });
    });

    it('should call request with PUT method and body', async () => {
      const body = { name: 'Test' };
      await apiClient.put('/test', body);
      expect(apiClient.request).toHaveBeenCalledWith('/test', {
        method: 'PUT',
        body: JSON.stringify(body),
      });
    });

    it('should call request with DELETE method', async () => {
      await apiClient.delete('/test');
      expect(apiClient.request).toHaveBeenCalledWith('/test', {
        method: 'DELETE',
      });
    });
  });
});
