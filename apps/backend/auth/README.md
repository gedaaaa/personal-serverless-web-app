# Auth Service

This is a backend authentication service built with Kotlin and Micronaut.

## Features

- Health check endpoint at `/health`
- More features to be added

## Development

To run the service locally in development environment:

```bash
# Run in development environment using NX
pnpm nx run auth-service:dev

# Or use SAM local API gateway
cd /workspace
./local-env/scripts/start-local.sh
```

When using SAM local API gateway, the service will be available at `http://localhost:3000/auth/health`.

## Environment Configuration

The service supports different environment configurations:

- **Default**: Base configuration in `application.yml`
- **Development**: Development-specific settings in `application-development.yml`
- **Production**: Production-specific settings in `application-production.yml`

Environment configuration is set through:

- Local development: Set to `development` via environment variables in SAM template
- Production: Set to `production` via environment variables in CDK configuration

## Testing

To run tests:

```bash
# Using Gradle
./gradlew :apps:backend:auth:app:test

# Or using NX
pnpm nx run auth-service:test
```

## Deployment

To deploy the service:

```bash
pnpm nx run auth-service:deploy
```

## API Endpoints

### Health Check

- **URL**: `/health`, But we will have `/auth` prefix for this microservice when in production or in SAM API gateway.
- **Method**: `GET`
- **Response Example**:
  ```json
  {
    "status": "UP",
    "service": "auth",
    "timestamp": "2023-06-01T12:00:00.000"
  }
  ```

## Deployment

### Deploy to AWS using CDK

The Auth service uses AWS CDK for deployment. CDK configuration files are located in the `infra` directory.

Deployment steps:

1. Build application JAR file:

```bash
nx run backend-auth:build
```

2. Compile CDK code:

```bash
cd apps/backend/auth/infra
./gradlew compileKotlin
```

3. Deploy to AWS:

```bash
cd apps/backend/auth/infra
cdk deploy
```

After deployment, CDK will output the API Gateway URL.
