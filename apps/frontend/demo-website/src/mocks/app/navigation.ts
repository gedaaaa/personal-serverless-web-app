import { vi } from 'vitest';

// Mock SvelteKit navigation functions
export const goto = vi.fn();
export const invalidate = vi.fn();
export const prefetch = vi.fn();
export const beforeNavigate = vi.fn();
export const afterNavigate = vi.fn();
