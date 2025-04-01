# Personal Serverless Web App

This is a monorepo for a personal serverless web application, built with NX, Svelte5/SvelteKit, and Micronaut.

For a personal web application, AWS Lambda is our preferred runtime as its free tier is expected to cover all associated costs. Additionally, when selecting other cloud services, we prioritize utilizing free-tier offerings or cost-effective pay-as-you-go pricing models to minimize expenses.

## Architecture Overview

This project uses a microservices architecture with the following components:

- **Frontend**: Svelte5/SvelteKit application deployed with SST
- **Backend**: Multiple Kotlin microservices deployed as AWS Lambda functions
- **Infrastructure**: AWS CDK for infrastructure as code

## Development Environment

The project uses Dev Container for development environment management, ensuring consistent environment setup.

### Prerequisites

- [VS Code](https://code.visualstudio.com/) or other IDE with Dev Container support
- [Docker](https://www.docker.com/)
- [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers) VS Code extension

### Getting Started

1. After cloning the project, open it in VS Code
2. When prompted, select "Reopen in Container"
3. The Dev Container will automatically install all necessary dependencies and tools

## Project Structure Example

```
├── apps/                  # Applications
│   ├── frontend/         # Frontend applications
│   ├── backend/         # Backend microservices
├── libs/                # Shared libraries
```

## Development Guide

### Local Development

There are two ways to run services:

1. Using NX (for developing individual services)

```bash
# Run a specific service in development mode
pnpm nx run <service-name>:dev
```

2. Using SAM (for host backend services locally with single endpoint)

```bash
# Start all backend services using SAM
pnpm start:backend
```

### Testing

```bash
# Run tests for a specific project
pnpm nx run <project-name>:test
```

### Deployment

```bash
# Deploy a specific service
pnpm nx run <service-name>:deploy
```

