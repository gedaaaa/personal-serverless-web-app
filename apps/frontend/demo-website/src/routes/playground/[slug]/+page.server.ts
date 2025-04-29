import { error } from '@sveltejs/kit';

// Load function that handles exsistence checking.
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

  return {};
}

// Entries function that returns a list of all possible slugs.
export async function entries() {
  const modules = import.meta.glob('../markdowns/*.{svx,md}');
  return Object.keys(modules).map((path) => ({
    slug: path.replace('../markdowns/', '').replace(/\.(svx|md)$/, ''),
  }));
}

export const prerender = true;
