import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import UserList from '../index.svelte';
import { userListStore } from '../store';
import { tick } from 'svelte';
import type { User } from '../../../_services/userService';
import type { ComponentType, SvelteComponent } from 'svelte';
// Define component t
// Mock dependencies
vi.mock('../store', () => ({
  userListStore: {
    subscribe: vi.fn(),
    loadUsers: vi.fn().mockResolvedValue([]),
    updateUser: vi.fn().mockImplementation(() => Promise.resolve({})),
    deleteUser: vi.fn().mockImplementation(() => Promise.resolve({})),
  },
}));

describe('UserList Component', () => {
  // Mock store data
  const mockUsers: User[] = [
    {
      id: '1',
      username: 'testuser1',
      email: 'test1@example.com',
      fullName: 'Test User 1',
      roles: ['ROLE_USER'],
    },
    {
      id: '2',
      username: 'testuser2',
      email: 'test2@example.com',
      fullName: 'Test User 2',
      roles: ['ROLE_ADMIN'],
    },
  ];

  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks();

    // Setup store mock with default state
    vi.mocked(userListStore.subscribe).mockImplementation((callback) => {
      callback({
        users: mockUsers,
        error: null,
      });
      return () => {};
    });
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Initial Render', () => {
    it('should render user list table with correct headers', () => {
      render(UserList as unknown as ComponentType<SvelteComponent>);

      // Check for table headers
      expect(screen.getByText('用户名')).toBeTruthy();
      expect(screen.getByText('邮箱')).toBeTruthy();
      expect(screen.getByText('姓名')).toBeTruthy();
      expect(screen.getByText('角色')).toBeTruthy();
      expect(screen.getByText('操作')).toBeTruthy();
    });

    it('should render user list items', () => {
      render(UserList as unknown as ComponentType<SvelteComponent>);

      // Check for user list items
      expect(screen.getByText('testuser1')).toBeTruthy();
      expect(screen.getByText('test1@example.com')).toBeTruthy();
      expect(screen.getByText('Test User 1')).toBeTruthy();
      expect(screen.getByText('User')).toBeTruthy();

      expect(screen.getByText('testuser2')).toBeTruthy();
      expect(screen.getByText('test2@example.com')).toBeTruthy();
      expect(screen.getByText('Test User 2')).toBeTruthy();
      expect(screen.getByText('Admin')).toBeTruthy();
    });

    it('should load users on mount', () => {
      render(UserList as unknown as ComponentType<SvelteComponent>);

      // Check that loadUsers was called
      expect(userListStore.loadUsers).toHaveBeenCalledWith(10);
    });
  });

  describe('Empty State', () => {
    it('should display empty state message when no users', () => {
      // Setup store mock with empty users
      vi.mocked(userListStore.subscribe).mockImplementation((callback) => {
        callback({
          users: [],
          error: null,
        });
        return () => {};
      });

      render(UserList as unknown as ComponentType<SvelteComponent>);

      // Check for empty state message
      expect(screen.getByText('暂无用户数据')).toBeTruthy();
    });
  });

  describe('User Actions', () => {
    it('should open edit modal when edit button is clicked', async () => {
      render(UserList as unknown as ComponentType<SvelteComponent>);

      // Find and click edit button for first user
      const editButtons = screen.getAllByRole('button', { name: /edit/i });
      await fireEvent.click(editButtons[0]);
      await tick();

      // Check that edit modal is shown
      expect(screen.getByText('Edit User')).toBeTruthy();
    });

    it('should open delete modal when delete button is clicked', async () => {
      render(UserList as unknown as ComponentType<SvelteComponent>);

      // Find and click delete button for first user
      const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
      await fireEvent.click(deleteButtons[0]);
      await tick();

      // Check that delete modal is shown
      expect(screen.getByText('Delete User')).toBeTruthy();
    });

    it('should handle user update through modal', async () => {
      render(UserList as unknown as ComponentType<SvelteComponent>);

      // Open edit modal
      const editButtons = screen.getAllByRole('button', { name: /edit/i });
      await fireEvent.click(editButtons[0]);
      await tick();

      // Fill in edit form
      const emailInput = screen.getByLabelText('Email');
      await fireEvent.input(emailInput, {
        target: { value: 'newemail@example.com' },
      });
      await tick();

      // Submit form
      const submitButton = screen.getByRole('button', { name: 'Save' });
      await fireEvent.click(submitButton);
      await Promise.resolve();

      // Check that updateUser was called
      expect(userListStore.updateUser).toHaveBeenCalledWith('1', {
        email: 'newemail@example.com',
        fullName: undefined,
        password: undefined,
        roles: ['ROLE_USER'],
      });
    });

    it('should handle user delete through modal', async () => {
      render(UserList as unknown as ComponentType<SvelteComponent>);

      // Open delete modal
      const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
      await fireEvent.click(deleteButtons[0]);
      await tick();

      // Confirm deletion
      const confirmButtons = screen.getAllByRole('button', { name: /delete/i });
      await fireEvent.click(confirmButtons[1]);
      await Promise.resolve();

      // Check that deleteUser was called
      expect(userListStore.deleteUser).toHaveBeenCalledWith('1');
    });
  });
});
