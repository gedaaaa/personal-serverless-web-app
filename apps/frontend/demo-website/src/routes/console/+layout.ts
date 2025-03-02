import { browser } from '$app/environment';
import { redirect } from '@sveltejs/kit';
import { isAuthenticated } from '$lib/auth';
import { isAdmin } from '$lib/auth/permissions';

export const load = () => {
  if (browser) {
    // 检查用户是否已登录，如果未登录则重定向到登录页面
    if (!isAuthenticated()) {
      throw redirect(302, '/');
    }

    // 检查用户是否是管理员，如果不是则重定向到首页
    if (!isAdmin()) {
      throw redirect(302, '/');
    }
  }

  return {};
};

// 确保服务器端和客户端都会重新运行这个加载函数
export const ssr = false;
export const csr = true;
