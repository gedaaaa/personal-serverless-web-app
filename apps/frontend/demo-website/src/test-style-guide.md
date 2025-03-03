# Frontend Testing Style Guide

> Summary: This guide establishes unified standards for frontend testing. Key aspects include: using English comments, following the AAA (Arrange-Act-Assert) pattern, standardizing assertion styles (using toBeTruthy instead of toBeInTheDocument), resetting all mock objects in beforeEach and afterEach, and maintaining consistent import structures.

This document outlines the standard testing practices for our frontend codebase to ensure consistency across all test files.

## Naming Conventions

### File Naming

- Test files should be placed next to the file they are testing
- Test file naming conventions:
  - Svelte component tests: `-page.test.svelte.ts` or `Component.test.svelte.ts` (for testing UI components)
  - Component logic tests: `-page.test.ts` or `Component.test.ts` (for testing business logic)
  - Service and utility tests: `[name].test.ts`

### Test Description Naming

- Top-level `describe` blocks:
  - For components: `describe('[Name] Page', () => {...}` or `describe('[Name] Component', () => {...}`
  - For services: `describe('[ServiceName]', () => {...}`
  - For modules: `describe('[ModuleName] Module', () => {...}`
- Nested `describe` blocks should group related functionality: `describe('Initial Rendering', () => {...}`
- Test methods (`it` blocks) should:
  - Always start with "should"
  - Clearly describe the expected behavior
  - Be concise (if a test description is too long, consider using nested `describe` blocks)
  - Examples: `it('should render welcome message', () => {...}`, `it('should call API on button click', () => {...}`

## General Guidelines

- Use English for all comments and test descriptions
- Follow the AAA pattern (Arrange, Act, Assert) with comments to separate sections

## Import Structure

```typescript
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { waitFor } from '@testing-library/dom';
// Import the component or module being tested
import Component from './Component.svelte';
// Import dependencies that need to be mocked
import { SomeService } from '$lib/services/some-service';
```

## Mocking

### Mocking Dependencies

```typescript
// Mock dependencies
vi.mock('$lib/services/some-service', () => {
  const SomeService = vi.fn();
  SomeService.prototype.someMethod = vi.fn();

  return {
    SomeService,
  };
});
```

### Mocking Browser APIs

```typescript
// Mock localStorage
const mockLocalStorage = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
    clear: vi.fn(() => {
      store = {};
    }),
  };
})();

global.localStorage = mockLocalStorage as unknown as Storage;
```

## Test Structure

```typescript
describe('ComponentName', () => {
  // Setup variables
  let mockService: MockService;

  beforeEach(() => {
    // Initialize test environment
    mockService = new SomeService() as MockService;

    // Reset all mocks
    vi.resetAllMocks();

    // Setup default mock responses
    mockService.someMethod.mockResolvedValue({ data: 'test' });
  });

  afterEach(() => {
    // Clean up after each test
    vi.resetAllMocks();
  });

  describe('featureName', () => {
    it('should do something specific', async () => {
      // Arrange
      const testData = { id: '1', name: 'Test' };

      // Act
      render(Component, { props: { data: testData } });

      // Assert
      expect(screen.getByText('Test')).toBeTruthy();
    });
  });
});
```

## Assertions

Use consistent assertion styles:

```typescript
// Preferred assertions for DOM elements
expect(element).toBeTruthy();
expect(screen.getByText('text')).toBeTruthy();

// For checking if something was called
expect(mockFn).toHaveBeenCalled();
expect(mockFn).toHaveBeenCalledWith(arg1, arg2);

// For checking values
expect(result).toEqual(expectedValue);
expect(value).toBe(expectedValue);
```

## Async Testing

```typescript
it('should handle async operations', async () => {
  // Arrange
  mockService.getData.mockResolvedValue({ result: 'success' });

  // Act
  render(Component);
  await fireEvent.click(screen.getByRole('button'));

  // Assert
  await waitFor(() => {
    expect(screen.getByText('success')).toBeTruthy();
  });
});
```

## Component Testing

When testing Svelte components:

1. Use `render` from `@testing-library/svelte`
2. Use `screen` to query elements
3. Use `fireEvent` for user interactions
4. Use `waitFor` for async operations

```typescript
it('should update when button is clicked', async () => {
  // Arrange
  render(Component);

  // Act
  await fireEvent.click(screen.getByRole('button', { name: 'Update' }));

  // Assert
  expect(screen.getByText('Updated')).toBeTruthy();
});
```

## Service Testing

When testing services:

1. Mock API clients or other dependencies
2. Test each method separately
3. Verify correct parameters are passed to dependencies
4. Test error handling

```typescript
it('should call API with correct parameters', async () => {
  // Arrange
  mockApiClient.get.mockResolvedValue({ data: 'result' });

  // Act
  const result = await service.getData(123);

  // Assert
  expect(mockApiClient.get).toHaveBeenCalledWith('/api/data/123');
  expect(result).toEqual({ data: 'result' });
});
```
