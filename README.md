# Personal Todo List Webapp

This is a monorepo for a personal todo list web application, built with NX, Micronaut, and AWS Lambda.

## Architecture Overview

This project uses a microservices architecture with the following components:

- **Frontend**: Angular application
- **Backend**: Multiple Kotlin microservices deployed as AWS Lambda functions
- **Infrastructure**: AWS CDK for infrastructure as code

### Development Best Practices

This project follows these development best practices:

- **Single Responsibility Principle**: Each microservice focuses on a single business domain
- **Environment Isolation**: Development, testing, and production environments are strictly separated
- **Infrastructure as Code**: All infrastructure is defined and managed through CDK
- **Local Development Strategy**:
  - When developing a specific service, prefer using NX directly (supports hot reloading)
  - When frontend development or other services need to call backend services, use SAM to provide a unified API entry point
- **Automated Deployment**: One-click build and deployment through NX commands

## Project Structure

```
├── apps/                  # Applications
│   ├── frontend/          # Frontend applications
│   ├── backend/           # Backend microservices
│       ├── helloworld/    # Hello World service
│       ├── auth/          # Authentication service
├── local-env/             # Local development environment
│   ├── scripts/           # Scripts for local development
│   ├── templates/         # SAM templates
├── packages/              # Shared libraries
```

## Development Guide

### Prerequisites

- [Node.js](https://nodejs.org/) and [pnpm](https://pnpm.io/)
- [Java 21](https://adoptium.net/)
- [AWS CLI](https://aws.amazon.com/cli/)
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- [AWS CDK](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html)
- [Docker](https://www.docker.com/) (for local development)

### Local Development

There are two ways to run services locally:

#### 1. Using NX (for developing individual services)

```bash
# Run a specific service in development mode
pnpm nx run <service-name>:dev

# Example: Run the helloworld service
pnpm nx run helloworld-service:dev
```

This method supports hot reloading, making it ideal for focused development on a specific service.

#### 2. Using SAM (for integration testing)

```bash
# Start all backend services using SAM
pnpm start:backend

# Or run the script directly
./local-env/scripts/start-local.sh
```

This method provides a unified API entry point at `http://localhost:3000` for all microservices, making it ideal for frontend development or testing service integration.

### Testing

```bash
# Run tests for a specific project
pnpm nx run <project-name>:test

# Example: Test the helloworld service
pnpm nx run helloworld-service:test
```

### Deployment

```bash
# Deploy a specific service
pnpm nx run <service-name>:deploy

# Example: Deploy the helloworld service
pnpm nx run helloworld-service:deploy
```

## Environment Notes

Required development environment components:

- node, pnpm, jdk, micronaut, cdk(ci) aws(ci), docker(dev)
- python3 venv direnv for automatically switching to venv with SAM

The `local-env` directory contains scripts to start the local backend development environment. The package.json defines scripts to run these scripts and reload the server.

rm -rf ./apps/backend/auth/app/.micronaut/test-resources/

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
│   │   ├── store.ts            # Component state management (if needed)
│   │   ├── internal/           # Internal sub-components
│   │   │   ├── SubComponent/   # Each sub-component in its own directory
│   │   │   │   ├── index.svelte  # Sub-component implementation
│   │   │   │   ├── index.ts      # Sub-component exports (optional)
│   │   │   │   └── SubComponent.svelte  # Simple sub-components
│   │   │   └── ...
│   │   └── __tests__/          # Tests for this component
│   │       └── ComponentName.spec.ts
│   └── ...
└── _services/                  # Service layer for API calls
    └── serviceName.ts
```

### Key Principles

1. **Directory as Component**: Top-level and major sub-components live in their own directories.
2. **Simple Sub-components**: Deeply nested or simple sub-components can be single files.
3. **Underscore Prefix**: Use `_` prefix for non-route directories to prevent SvelteKit from treating them as routes.
4. **Separation of Concerns**:
   - UI components in `_components/`
   - API calls in `_services/`
5. **Component Isolation**: Components should only import from their own directory or from explicitly exported APIs.
6. **Internal Components**: Sub-components that are only used by a parent component should be in the `internal/` directory.
7. **Testing**: Each component should have its own tests in a `__tests__/` directory.

### State Management

We use Svelte's built-in stores for state management:

1. Create stores in the component's directory if the state is specific to that component
2. Keep stores close to where they are used
3. Export only the necessary store interface

Example:

```typescript
// ComponentName/store.ts
function createStore() {
  const state = writable<State>({});

  return {
    subscribe: state.subscribe,
    action1: () => {
      /* ... */
    },
    action2: () => {
      /* ... */
    },
  };
}

export const store = createStore();
```

### Import Conventions

1. **External Imports**: Import from the component's public API (index.ts)

   ```typescript
   import ComponentName from '../ComponentName';
   ```

2. **Internal Imports**: Import directly from the file

   ```typescript
   import SubComponent from './internal/SubComponent/index.svelte';
   ```

3. **Store Imports**: Import from the component's store file
   ```typescript
   import { componentStore } from './store';
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

By following these guidelines, we ensure a consistent, maintainable, and scalable frontend codebase.
