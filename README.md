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

# PersonalTodoListWebapp
