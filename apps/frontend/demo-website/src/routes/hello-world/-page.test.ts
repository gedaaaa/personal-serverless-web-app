import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import Page from './+page.svelte';
import { ApiClient } from '$lib/api';
import * as authModule from '$lib/auth';
import { tick } from 'svelte';

// Mock dependencies
vi.mock('$lib/api', () => ({
  ApiClient: vi.fn(),
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

describe('Hello World Page', () => {
  // Mock API client
  const mockApiClient = {
    get: vi.fn(),
    setAuthToken: vi.fn(),
  };

  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks();

    // Setup API client mock
    (ApiClient as unknown as ReturnType<typeof vi.fn>).mockImplementation(
      () => mockApiClient,
    );
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  it('should render welcome message with username', () => {
    render(Page);

    // Check for welcome message with username - using more specific selectors
    const welcomeParagraph = screen.getByText(/Welcome,/);
    expect(welcomeParagraph).toBeTruthy();
    expect(screen.getByText('testuser')).toBeTruthy();
  });

  it('should show initial state with prompt to say hello', () => {
    render(Page);

    // Check for initial prompt
    expect(screen.getByText('Do you want to say hello?')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Say Hello' })).toBeTruthy();
  });

  it('should call API and display greeting when "Say Hello" button is clicked', async () => {
    // Setup successful API response
    mockApiClient.get.mockResolvedValue({ message: 'Hello from server!' });

    render(Page);

    // Click the Say Hello button
    const sayHelloButton = screen.getByRole('button', { name: 'Say Hello' });
    await fireEvent.click(sayHelloButton);

    // Wait for promises to resolve
    await tick();

    // Check that API was called with correct path
    expect(mockApiClient.get).toHaveBeenCalledWith('/hello-world/greeting');

    // Check that greeting is displayed
    expect(screen.getByText(/Server says:/)).toBeTruthy();
    expect(screen.getByText('Hello from server!')).toBeTruthy();
  });

  it('should display error message when API call fails', async () => {
    // Setup failed API response
    mockApiClient.get.mockRejectedValue(new Error('API error'));

    render(Page);

    // Click the Say Hello button
    const sayHelloButton = screen.getByRole('button', { name: 'Say Hello' });
    await fireEvent.click(sayHelloButton);

    // Wait for promises to resolve
    await tick();

    // Check that error message is displayed
    expect(screen.getByText('Server is quite mad now!')).toBeTruthy();
  });

  it('should call logout function when logout button is clicked', async () => {
    render(Page);

    // Click the logout button
    const logoutButton = screen.getByRole('button', { name: 'Logout' });
    await fireEvent.click(logoutButton);

    // Check that logout was called with correct path
    expect(authModule.logout).toHaveBeenCalledWith('/');
  });
});
