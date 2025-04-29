import { error } from '@sveltejs/kit';

// Load function that handles exsistence checking and load Mdsvex or Markdown content.
export async function load({ params }) {
  const modules = import.meta.glob('../markdowns/*.{svx,md}');
  const slugs = Object.keys(modules).map((path) =>
    path.replace('../markdowns/', '').replace(/\.(svx|md)$/, ''),
  );

  if (!slugs.includes(params.slug)) {
    throw error(404, {
      message: `Path ${params.slug} not found`,
    });
  }

  try {
    // Load the content
    let post;
    try {
      post = await import(`../markdowns/${params.slug}.svx`);
    } catch {
      post = await import(`../markdowns/${params.slug}.md`);
    }

    return {
      Content: post.default,
      metadata: {
        ...post.metadata,
        ogImage: `/og?category=playground&slug=${params.slug}`,
      },
    };
  } catch {
    throw error(404, 'Not found');
  }
}

// Entries function that returns a list of all possible slugs.
export function entries() {
  const modules = import.meta.glob('../markdowns/*.{svx,md}');
  return Object.keys(modules).map((path) => ({
    slug: path.replace('../markdowns/', '').replace(/\.(svx|md)$/, ''),
  }));
}

export const prerender = true;
