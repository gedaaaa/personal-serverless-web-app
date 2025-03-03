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

## Frontend Component Guidelines

### Directory Structure Pattern

We follow a "directory as component" pattern for organizing our frontend components, with a focus on separation of concerns and maintainability.

```
/routes/<route-path>/
├── +page.svelte                # Main page component
├── _components/                # Components specific to this route (underscore prefix to avoid route conflicts)
│   ├── ComponentName/          # Each component in its own directory
│   │   ├── index.svelte        # Main component implementation
│   │   ├── index.ts            # Public API exports (optional)
│   │   ├── internal/           # Internal sub-components
│   │   │   ├── SubComponent/   # Each sub-component in its own directory
│   │   │   │   └── index.svelte
│   │   │   └── ...
│   │   └── __tests__/          # Tests for this component
│   │       └── ComponentName.spec.ts
│   └── ...
├── _contexts/                  # Context providers for state management
│   └── contextName.ts
└── _services/                  # Service layer for API calls
    └── serviceName.ts
```

### Key Principles

1. **Directory as Component**: Each component lives in its own directory with all related files.
2. **Underscore Prefix**: Use `_` prefix for non-route directories to prevent SvelteKit from treating them as routes.
3. **Separation of Concerns**:
   - UI components in `_components/`
   - State management in `_contexts/`
   - API calls in `_services/`
4. **Component Isolation**: Components should only import from their own directory or from explicitly exported APIs.
5. **Internal Components**: Sub-components that are only used by a parent component should be in the `internal/` directory.
6. **Testing**: Each component should have its own tests in a `__tests__/` directory.

### State Management

We use Svelte's Context API for state management:

1. Create a context in `_contexts/` directory
2. Set the context in the main page component
3. Components access the context using `getContext()`

### Import Conventions

1. **External Imports**: Import from the component's public API (index.ts)

   ```typescript
   import ComponentName from '../ComponentName';
   ```

2. **Internal Imports**: Import directly from the file

   ```typescript
   import SubComponent from './internal/SubComponent/index.svelte';
   ```

3. **Context Imports**: Import from the context file
   ```typescript
   import { CONTEXT_KEY, type ContextType } from '../../_contexts/contextName';
   ```

### Component API Design

1. **Props**: Define props at the top of the component script

   ```svelte
   <script lang="ts">
     export let propName: PropType;
   </script>
   ```

2. **Events**: Use Svelte's event dispatching

   ```svelte
   <script lang="ts">
     import { createEventDispatcher } from 'svelte';
     const dispatch = createEventDispatcher<{
       event: EventType;
     }>();
   </script>
   ```

3. **Slots**: Use named slots for complex components
   ```svelte
   <div>
     <slot name="header"></slot>
     <slot></slot>
     <slot name="footer"></slot>
   </div>
   ```

### Example Implementation

For a user management page, the structure would look like:

```
/routes/console/users/
├── +page.svelte                # Main user management page
├── _components/                # User-specific components
│   ├── UserList/               # User list component
│   │   ├── index.svelte        # Main implementation
│   │   ├── index.ts            # Exports
│   │   ├── internal/           # Internal components
│   │   │   └── UserListItem/   # List item component
│   │   │       └── index.svelte
│   │   └── __tests__/
│   │       └── UserList.spec.ts
│   ├── UserEditModal/          # Edit user modal
│   │   └── ...
│   └── UserDeleteModal/        # Delete confirmation modal
│       └── ...
├── _contexts/
│   └── userManagement.ts       # User management context
└── _services/
    └── userService.ts          # User API service
```

By following these guidelines, we ensure a consistent, maintainable, and scalable frontend codebase.
