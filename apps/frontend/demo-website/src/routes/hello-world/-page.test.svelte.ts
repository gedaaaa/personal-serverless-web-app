import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { waitFor } from '@testing-library/dom';
import HelloWorldPage from './+page.svelte';
import { getDefaultClient } from '$lib/api/client';
import * as authModule from '$lib/auth';

// Mock dependencies
vi.mock('$lib/api/client', () => ({
  getDefaultClient: vi.fn(),
}));

vi.mock('$lib/auth', () => ({
  auth: {
    subscribe: vi.fn((callback) => {
      callback({
        token: 'mock-token',
        user: { id: 'user-123', username: 'testuser' },
        isAuthenticated: true,
      });
      return () => {};
    }),
  },
  logout: vi.fn(),
}));

describe('HelloWorldPage Component', () => {
  // Mock API client
  const mockApiClient = {
    get: vi.fn(),
    setAuthToken: vi.fn(),
  };

  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks();

    // Setup API client mock
    (
      getDefaultClient as unknown as ReturnType<typeof vi.fn>
    ).mockImplementation(() => mockApiClient);
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  it('should render the component with correct structure', () => {
    const { container } = render(HelloWorldPage as any);

    // Check for main elements
    expect(screen.getByText('Hello World')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Logout' })).toBeTruthy();

    // Check for welcome message - using a more specific approach
    const welcomeParagraph = screen.getByText(/Welcome,/);
    expect(welcomeParagraph).toBeTruthy();
    expect(screen.getByText('testuser')).toBeTruthy();

    // Check for initial state
    expect(screen.getByText('Do you want to say hello?')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Say Hello' })).toBeTruthy();

    // Check for proper structure
    const mainDiv = container.querySelector('div.mx-auto');
    expect(mainDiv).toBeTruthy();
  });

  it('should handle successful API call', async () => {
    // Setup successful API response
    mockApiClient.get.mockResolvedValue({ message: 'Hello from server!' });

    render(HelloWorldPage as any);

    // Initial state check
    expect(screen.getByText('Do you want to say hello?')).toBeTruthy();

    // Click the Say Hello button
    const sayHelloButton = screen.getByRole('button', { name: 'Say Hello' });
    await fireEvent.click(sayHelloButton);

    // Check API call
    expect(mockApiClient.get).toHaveBeenCalledWith('/hello-world/v1/greeting');

    // Check updated UI
    await waitFor(() => {
      expect(screen.getByTestId('server-response')).toBeTruthy();
    });
    expect(screen.getByText('Hello from server!')).toBeTruthy();
  });

  it('should handle API error', async () => {
    // Setup failed API response
    mockApiClient.get.mockRejectedValue(new Error('API error'));

    render(HelloWorldPage as any);

    // Click the Say Hello button
    const sayHelloButton = screen.getByRole('button', { name: 'Say Hello' });
    await fireEvent.click(sayHelloButton);

    // Check error message
    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toBeTruthy();
    });
    expect(screen.getByTestId('error-message').textContent).toBe(
      'Server is quite mad now!',
    );
  });

  it('should handle logout', async () => {
    render(HelloWorldPage as any);

    // Click logout button
    const logoutButton = screen.getByRole('button', { name: 'Logout' });
    await fireEvent.click(logoutButton);

    // Check logout function was called
    expect(authModule.logout).toHaveBeenCalledWith('/login');
  });
});
