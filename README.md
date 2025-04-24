# Personal Serverless Web

## Project Goals

This repository serves two primary objectives:

1. **Rapid Prototyping Platform**: Create a web application framework for quickly implementing personal utilities, blog systems, and experimental concepts
2. **Technology Exploration**: Use languages and frameworks that I haven't tryed/worked before

### Key Constraints

- **Cost Optimization**: Maintain near-zero operational costs (current average: $0.01/month for storage)
- **Serverless Architecture**: Leverage cloud free-tier services exclusively

## Technical Stack

| Category     | Technologies                           |
| ------------ | -------------------------------------- |
| **Frontend** | Svelte 5/SvelteKit with SST deployment |
| **Backend**  | Kotlin/Micronaut microservices         |
| **Cloud**    | AWS Lambda (Free Tier)                 |
| **CDN**      | Cloudflare Free Plan                   |
| **Infra**    | AWS CDK for IaC                        |

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
pnpm nx run root:gateway-start
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

Live demo: [https://sunbath.top](https://sunbath.top)
