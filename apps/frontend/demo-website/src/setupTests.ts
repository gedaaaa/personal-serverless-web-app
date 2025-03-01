// Setup file for Vitest
import { vi } from 'vitest';
import '@testing-library/jest-dom';

// Mock SvelteKit modules
vi.mock('$app/environment', () => ({
  browser: true,
}));
