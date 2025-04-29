export async function load() {
  const modules = import.meta.glob<{
    metadata: {
      title: string;
      date: string;
      tags: string[];
      description: string;
    };
  }>('./markdowns/*.{svx,md}');

  const posts = [];
  for (const path in modules) {
    const { metadata } = await modules[path]();
    const slug = path.replace('./markdowns/', '').replace(/\.(svx|md)$/, '');
    posts.push({
      slug,
      ...metadata,
    });
  }

  posts.sort((a, b) => {
    const dateA = new Date(a.date);
    const dateB = new Date(b.date);
    return dateB.getTime() - dateA.getTime();
  });

  return { posts };
}
