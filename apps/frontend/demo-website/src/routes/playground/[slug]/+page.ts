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

  // Load the content
  let post;
  try {
    post = await import(`../markdowns/${params.slug}.svx`);
  } catch (e) {
    if (import.meta.env.DEV) {
      console.error(e);
    }
    try {
      post = await import(`../markdowns/${params.slug}.md`);
    } catch (e) {
      if (import.meta.env.DEV) {
        console.error(e);
      }
      throw error(404, 'Not found');
    }
  }

  return {
    Content: post.default,
    metadata: {
      ...post.metadata,
      ogImage: `/og?category=playground&slug=${params.slug}`,
    },
  };
}

// Entries function that returns a list of all possible slugs.
export function entries() {
  const modules = import.meta.glob('../markdowns/*.{svx,md}');
  return Object.keys(modules).map((path) => ({
    slug: path.replace('../markdowns/', '').replace(/\.(svx|md)$/, ''),
  }));
}

export const prerender = true;
