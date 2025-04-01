# sv

Everything you need to build a Svelte project, powered by [`sv`](https://github.com/sveltejs/cli).

## Creating a project

If you're seeing this, you've probably already done this step. Congrats!

```bash
# create a new project in the current directory
npx sv create

# create a new project in my-app
npx sv create my-app
```

## Developing

Once you've created a project and installed dependencies with `npm install` (or `pnpm install` or `yarn`), start a development server:

```bash
npm run dev

# or start the server and open the app in a new browser tab
npm run dev -- --open
```

## Building

To create a production version of your app:

```bash
npm run build
```

You can preview the production build with `npm run preview`.

> To deploy your app, you may need to install an [adapter](https://svelte.dev/docs/kit/adapters) for your target environment.

# Frontend Development Guidelines

## Directory Structure Pattern

We follow a "directory as component" pattern for organizing our frontend components:

```
/routes/<route-path>/
├── +page.svelte                # Main page component
├── _components/                # Components specific to this route
│   ├── ComponentName/          # Each component in its own directory
│   │   ├── index.svelte        # Main component implementation
│   │   ├── index.ts            # Public API exports (optional)
│   │   ├── store.ts            # Component state management (if needed)
│   │   └── __tests__/          # Tests for this component
│   └── ...
└── _services/                  # Service layer for API calls
    └── serviceName.ts
```

## Key Principles

1. **Directory as Component**: Top-level components live in their own directories
2. **Separation of Concerns**:
   - UI components in `_components/`
   - API calls in `_services/`
3. **Component Isolation**: Components should only import from their own directory or from explicitly exported APIs
4. **Testing**: Each component should have its own tests in a `__tests__/` directory

## State Management

Using Svelte5's Runes API for state management:

```typescript
// Component state
let count = $state(0);

// Bindable state
let value = $bindable(initialValue);

// Derived state
let doubled = $derived(count * 2);

// Side effects
$effect(() => {
  console.log(`Count changed to ${count}`);
});
```

## Component API Design

Using Svelte5's props API:

```typescript
const { title, onAction } = $props<{
  title: string;
  onAction: () => void;
}>();
```

## Child Component Rendering

Using Svelte5's @render syntax:

```typescript
let { children } = $props<{
  children: unknown;
}>();

// In template
{@render children()}
```

## Import Conventions

1. **External Imports**: Import from the component's public API

   ```typescript
   import ComponentName from '../ComponentName';
   ```

2. **Internal Imports**: Import directly from the file
   ```typescript
   import SubComponent from './internal/SubComponent/index.svelte';
   ```
