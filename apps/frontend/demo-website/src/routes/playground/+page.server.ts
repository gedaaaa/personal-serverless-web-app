export async function load() {
  // 1. 扫描所有 .svx/.md 文件
  const modules = import.meta.glob<{
    metadata: { title: string; date: string };
  }>('./markdowns/*.{svx,md}');

  // 2. 提取元数据 (frontmatter)
  const posts = [];
  for (const path in modules) {
    const { metadata } = await modules[path]();
    const slug = path.replace('./markdowns/', '').replace(/\.(svx|md)$/, '');
    posts.push({
      slug,
      ...metadata,
    });
  }

  // 3. 按日期排序
  posts.sort((a, b) => {
    const dateA = new Date(a.date);
    const dateB = new Date(b.date);
    return dateB.getTime() - dateA.getTime();
  });

  return { posts };
}
