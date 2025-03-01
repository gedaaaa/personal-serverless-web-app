# Hello World Service

This is a backend microservice example built with Kotlin and Micronaut. This service demonstrates how to create, configure, and deploy a Micronaut-based AWS Lambda function.

## Architecture Overview

The Hello World service uses the following architecture:

- **Programming Language**: Kotlin
- **Framework**: Micronaut 4.7.4
- **Build Tool**: Gradle (Kotlin DSL)
- **Deployment Target**: AWS Lambda + API Gateway
- **Infrastructure as Code**: AWS CDK
- **Local Development Environment**: AWS SAM

### Development Best Practices

This project follows these development best practices:

- **Single Responsibility Principle**: Each microservice focuses on a single business domain
- **Environment Isolation**: Development, testing, and production environments are strictly separated
- **Infrastructure as Code**: All infrastructure is defined and managed through CDK
- **Local Development Strategy**:
  - When developing this service, prefer using NX directly (supports hot reloading)
  - When frontend development or other services need to call this service, use SAM to provide a unified API entry point
- **Automated Deployment**: One-click build and deployment through NX commands

### Directory Structure

```
apps/backend/helloworld/
├── app/                  # Application code
│   ├── src/              # Source code
│   ├── build.gradle.kts  # Gradle build configuration
├── infra/                # Infrastructure code
│   ├── src/              # CDK code
│   ├── build.gradle.kts  # Gradle build configuration
│   ├── cdk.json          # CDK configuration
├── project.json          # NX project configuration
├── README.md             # Project documentation
```

## Environment Configuration

The service supports different environment configurations:

- **Default**: Base configuration in `application.yml`
- **Development**: Development-specific settings in `application-development.yml`
- **Production**: Production-specific settings in `application-production.yml`

Environment configuration is set through:
- **Local Development**: Set to `development` via environment variable in the SAM template
- **Production**: Set to `production` via environment variable in the CDK configuration

## Development Guide

### Prerequisites

- [Java 21](https://adoptium.net/)
- [AWS CLI](https://aws.amazon.com/cli/)
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- [AWS CDK](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html)
- [Node.js](https://nodejs.org/) and [pnpm](https://pnpm.io/)

### Local Development

There are two recommended ways to run the service locally, depending on the development scenario:

#### 1. Using NX (Recommended for Developing This Service)

```bash
# Run in development environment using NX
pnpm nx run helloworld-service:dev
```

**Advantages**:
- Supports hot reloading, automatically restarts after code changes
- Smoother development experience, faster feedback
- Suitable for focused development and debugging of this service

This is the preferred method when you are primarily developing and modifying the Hello World service itself.

#### 2. Using SAM Local API Gateway (Recommended for Integration Testing)

```bash
# Run from the project root
./local-env/scripts/start-local.sh
```

This will:
1. Build the application JAR file
2. Start a local API gateway, simulating the AWS environment
3. Set the `MICRONAUT_ENVIRONMENTS=development` environment variable

The service will be available at `http://localhost:3000/hello-world`.

**Advantages**:
- Provides a closer simulation of the AWS Lambda and API Gateway production environment
- Offers a unified API entry point for all microservices
- Suitable when frontend development or other backend services need to call this service

This is the preferred method when you need to test integration between multiple services, or when frontend development needs to call backend APIs.

### Testing

Run tests:

```bash
# Using Gradle
./gradlew :apps:backend:helloworld:app:test

# Or using NX
pnpm nx run helloworld-service:test
```

### Code Style Checking and Formatting

```bash
# Check code style
pnpm nx run helloworld-service:lint

# Format code
pnpm nx run helloworld-service:format
```

## Deployment Guide

### Build and Deploy to AWS

Use the NX command for one-click deployment:

```bash
pnpm nx run helloworld-service:deploy
```

This will:
1. Build an optimized JAR file
2. Deploy to AWS using CDK

### Multi-Environment Deployment

The project supports deployment to different environments (development, testing, production). Currently, the environment configuration is hardcoded as `production` in the CDK code, but multi-environment deployment can be implemented as follows:

1. Pass environment information using CDK context parameters:

```bash
# Deploy to development environment
cd apps/backend/helloworld/infra
cdk deploy -c environment=dev

# Deploy to production environment
cd apps/backend/helloworld/infra
cdk deploy -c environment=prod
```

2. Get the environment parameter in `AppStack.java`:

```java
String environment = this.getNode().tryGetContext("environment").toString();
environmentVariables.put("MICRONAUT_ENVIRONMENTS", environment);
```

This allows automatically setting the correct environment variable based on different deployment targets.

### Manual Deployment Steps

If you need more granular control, you can deploy manually using these steps:

1. Build the application JAR file:

```bash
./gradlew :apps:backend:helloworld:app:optimizedJitJarAll
```

2. Compile the CDK code:

```bash
cd apps/backend/helloworld/infra
./gradlew compileKotlin
```

3. Deploy to AWS:

```bash
cd apps/backend/helloworld/infra
cdk deploy
```

After deployment, CDK will output the API Gateway URL.

### Cleaning Up Resources

```bash
cd apps/backend/helloworld/infra
cdk destroy
```

## Infrastructure Configuration

### CDK Configuration

The service uses AWS CDK to define infrastructure. The main components include:

- **Lambda Function**: Handles API requests
  - Runtime: Java 21
  - Memory: 512 MB
  - Timeout: 10 seconds
  - SnapStart enabled to reduce cold start time
  - Environment variable: `MICRONAUT_ENVIRONMENTS=production`

- **API Gateway**: HTTP API integrated with Lambda
  - CORS configured
  - Custom domain mapping

- **Logs**: CloudWatch logs retained for one week

Detailed configuration can be found in `infra/src/main/java/top/sunbath/api/AppStack.java`.

### SAM Local Development Configuration

Local development uses AWS SAM to simulate the AWS environment. Configuration is located in `local-env/templates/template.yaml`, including:

- Lambda function configuration
- API Gateway routes
- Environment variable: `MICRONAUT_ENVIRONMENTS=development`

## API Endpoints

### Health Check

- **URL**: `/health` (production) or `/hello-world/health` (development)
- **Method**: `GET`
- **Response Example**:
  ```json
  {
    "status": "UP",
    "service": "hello-world",
    "timestamp": "2023-06-01T12:00:00.000"
  }
  ```

## Reference Documentation

- [Micronaut Documentation](https://docs.micronaut.io/4.7.4/guide/index.html)
- [AWS Lambda Handler](https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html)
- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/v2/guide/home.html)
- [Micronaut AWS Lambda Integration](https://micronaut-projects.github.io/micronaut-aws/latest/guide/index.html#lambda) 