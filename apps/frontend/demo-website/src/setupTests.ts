// Setup file for Vitest
import { vi } from 'vitest';
import '@testing-library/jest-dom';

// Mock SvelteKit modules
vi.mock('$app/environment', () => ({
  browser: true,
  dev: true,
}));

vi.mock('$app/navigation', () => ({
  goto: vi.fn(),
}));

vi.mock('$app/stores', () => {
  const page = {
    subscribe: vi.fn().mockImplementation((callback) => {
      callback({
        url: new URL('http://localhost'),
        params: {},
        route: { id: '' },
        status: 200,
        error: null,
        data: {},
        form: undefined,
        state: {},
      });
      return () => {};
    }),
  };
  return { page };
});
