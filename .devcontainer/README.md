# Dev Container Usage Guide

This project is configured with a Dev Container, allowing development in a containerized environment to ensure all developers use the same development environment. We combine Dev Container Features with project dependencies to provide a consistent development experience.

## Prerequisites

- Install [Docker](https://www.docker.com/products/docker-desktop)
- Install [Visual Studio Code](https://code.visualstudio.com/)
- Install VS Code extension [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

## How to Use

1. Clone the project to your local machine
2. Open the project in VS Code
3. When VS Code detects the `.devcontainer` directory, it will prompt you to reopen the project in a container. Click "Reopen in Container"
4. If no prompt appears, press `F1` or `Ctrl+Shift+P`, then type "Dev Containers: Reopen in Container"

## Container Environment Includes

Our development environment installs tools in two ways:

1. Dev Container Features (for main runtimes and tools)
2. Project dependencies (for frontend development tools)

### Runtime Environment

- Node.js 22 (via `ghcr.io/devcontainers/features/node:1`)
- Java 21 (via `ghcr.io/devcontainers/features/java:1`)

### Package Managers and Build Tools

- pnpm 9.15.5 (installed via `ghcr.io/devcontainers/features/node:1`)
- Gradle (automatically installed via `ghcr.io/devcontainers/features/java:1`)
- npm (installed with Node.js)

### Development Tools

- TypeScript (installed via project dependencies)
- ESLint (installed via project dependencies)
- Prettier (installed via project dependencies)
- Vite (installed via project dependencies)
- Nx (installed via project dependencies)
- Docker CLI (via `ghcr.io/devcontainers/features/docker-outside-of-docker:1`)
- AWS CLI (via `ghcr.io/devcontainers/features/aws-cli:1`)
- AWS SAM CLI (via `ghcr.io/audacioustux/devcontainers/aws-sam-cli:1`)
- GitHub CLI (via `ghcr.io/devcontainers/features/github-cli:1`)
- Svelte Check (installed via project dependencies)

### Environment Configuration

- Environment variables are set via `remoteEnv` to ensure executables are in the PATH

### VS Code Extensions

- Svelte
- Tailwind CSS
- ESLint
- Prettier
- Docker
- Angular Console
- Java Extension Pack
- AWS Toolkit
- Serverless IDE (for AWS SAM template editing)

## Port Forwarding

- 3000: Main application port
- 5173: Vite development server
- 8080: API server

## Local Development Workflow

1. Start your development servers according to the project requirements
2. Use the provided VS Code tasks or terminal commands to build and run the application

## Notes

- Project dependencies are automatically installed when the container starts
- File changes in the container are synchronized to the host
- To add new dependencies or tools, you can add them to the `features` section in `devcontainer.json` or as project dependencies in `package.json`
- We prioritize official Features:
  - Use official `devcontainers/features` to install main runtimes and tools
  - Use project dependencies to install frontend development tools (TypeScript, ESLint, Prettier, Vite, Nx, etc.)
- In the Dev Container, environment variables are set via `remoteEnv`
