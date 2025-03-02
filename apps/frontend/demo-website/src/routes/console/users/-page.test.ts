import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { waitFor } from '@testing-library/dom';
import UsersPage from './+page.svelte';
import { UserService } from '$lib/services/user-service';

// 模拟 UserService
vi.mock('$lib/services/user-service', () => {
  const UserService = vi.fn();
  UserService.prototype.getUsers = vi.fn();
  UserService.prototype.updateUser = vi.fn();
  UserService.prototype.deleteUser = vi.fn();

  return {
    UserService,
  };
});

describe('UsersPage', () => {
  let mockUserService: any;

  beforeEach(() => {
    mockUserService = new UserService();

    // 重置所有模拟函数
    vi.resetAllMocks();

    // 默认模拟返回值
    mockUserService.getUsers.mockResolvedValue({
      users: [
        {
          id: '1',
          username: 'admin',
          email: 'admin@example.com',
          roles: ['ROLE_USER', 'ROLE_ADMIN'],
          fullName: '系统管理员',
        },
        {
          id: '2',
          username: 'user1',
          email: 'user1@example.com',
          roles: ['ROLE_USER'],
          fullName: '测试用户 1',
        },
      ],
      nextCursor: null,
      hasMore: false,
    });
  });

  it('should render user list', async () => {
    // 渲染组件
    render(UsersPage);

    // 等待用户列表加载
    await waitFor(() => {
      expect(mockUserService.getUsers).toHaveBeenCalledWith(10, undefined);
    });

    // 验证用户列表显示
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
      expect(screen.getByText('admin@example.com')).toBeInTheDocument();
      expect(screen.getByText('系统管理员')).toBeInTheDocument();
      expect(screen.getByText('user1')).toBeInTheDocument();
      expect(screen.getByText('user1@example.com')).toBeInTheDocument();
      expect(screen.getByText('测试用户 1')).toBeInTheDocument();
    });
  });

  it('should show loading state', async () => {
    // 延迟 getUsers 的解析
    mockUserService.getUsers.mockImplementation(
      () =>
        new Promise((resolve) => {
          setTimeout(() => {
            resolve({
              users: [],
              nextCursor: null,
              hasMore: false,
            });
          }, 100);
        }),
    );

    // 渲染组件
    render(UsersPage);

    // 验证加载状态显示
    expect(screen.getByText('加载中...')).toBeInTheDocument();

    // 等待加载完成
    await waitFor(
      () => {
        expect(mockUserService.getUsers).toHaveBeenCalled();
      },
      { timeout: 200 },
    );
  });

  it('should show empty state', async () => {
    // 模拟空用户列表
    mockUserService.getUsers.mockResolvedValue({
      users: [],
      nextCursor: null,
      hasMore: false,
    });

    // 渲染组件
    render(UsersPage);

    // 等待用户列表加载
    await waitFor(() => {
      expect(mockUserService.getUsers).toHaveBeenCalled();
    });

    // 验证空状态显示
    await waitFor(() => {
      expect(screen.getByText('暂无用户数据')).toBeInTheDocument();
    });
  });

  it('should handle pagination', async () => {
    // 模拟分页数据
    mockUserService.getUsers
      .mockResolvedValueOnce({
        users: [
          {
            id: '1',
            username: 'user1',
            email: 'user1@example.com',
            roles: ['ROLE_USER'],
          },
        ],
        nextCursor: 'next-page',
        hasMore: true,
      })
      .mockResolvedValueOnce({
        users: [
          {
            id: '2',
            username: 'user2',
            email: 'user2@example.com',
            roles: ['ROLE_USER'],
          },
        ],
        nextCursor: null,
        hasMore: false,
      });

    // 渲染组件
    render(UsersPage);

    // 等待用户列表加载
    await waitFor(() => {
      expect(mockUserService.getUsers).toHaveBeenCalledWith(10, undefined);
    });

    // 验证"加载更多"按钮显示
    const loadMoreButton = await waitFor(() => screen.getByText('加载更多'));
    expect(loadMoreButton).toBeInTheDocument();

    // 点击"加载更多"按钮
    await fireEvent.click(loadMoreButton);

    // 验证第二次调用 getUsers
    await waitFor(() => {
      expect(mockUserService.getUsers).toHaveBeenCalledWith(10, 'next-page');
      expect(mockUserService.getUsers).toHaveBeenCalledTimes(2);
    });
  });
});
