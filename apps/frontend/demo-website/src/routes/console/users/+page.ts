// 这个页面的加载逻辑已经在 /console/+layout.ts 中处理
// 这里不需要额外的加载逻辑，因为已经在布局中确保了只有管理员可以访问

export const load = () => {
  return {};
};

// 确保服务器端和客户端都会重新运行这个加载函数
export const ssr = false;
export const csr = true;
