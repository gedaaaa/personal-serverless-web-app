# Auth Service

This is a backend authentication service built with Kotlin and Micronaut.

## Features

- Health check endpoint at `/health`
- More features to be added

## Development

To run the service locally in development environment:

```bash
# 使用NX在开发环境中运行
pnpm nx run auth-service:dev

# 或者使用SAM本地API网关
cd /workspace
./local-env/scripts/start-local.sh
```

在使用SAM本地API网关时，服务将在 `http://localhost:3000/auth/health` 上可用。

## Environment Configuration

The service supports different environment configurations:

- **Default**: Base configuration in `application.yml`
- **Development**: Development-specific settings in `application-development.yml`
- **Production**: Production-specific settings in `application-production.yml`

环境配置通过以下方式设置：
- 本地开发：通过SAM模板中的环境变量设置为 `development`
- 生产环境：通过CDK配置中的环境变量设置为 `production`

## Testing

To run tests:

```bash
# 使用Gradle
./gradlew :apps:backend:auth:app:test

# 或使用NX
pnpm nx run auth-service:test
```

## Deployment

To deploy the service:

```bash
pnpm nx run auth-service:deploy
```

## API Endpoints

### Health Check

- **URL**: `/health` (生产环境) 或 `/auth/health` (开发环境)
- **Method**: `GET`
- **Response Example**:
  ```json
  {
    "status": "UP",
    "service": "auth",
    "timestamp": "2023-06-01T12:00:00.000"
  }
  ```

## 部署

### 使用 CDK 部署到 AWS

Auth 服务使用 AWS CDK 进行部署。CDK 配置文件位于 `infra` 目录中。

部署步骤：

1. 构建应用程序 JAR 文件：

```bash
nx run backend-auth:build
```

2. 编译 CDK 代码：

```bash
cd apps/backend/auth/infra
./gradlew compileKotlin
```

3. 部署到 AWS：

```bash
cd apps/backend/auth/infra
cdk deploy
```

部署完成后，CDK 将输出 API 网关的 URL。

### 环境配置

- **开发环境**：本地开发时，通过 SAM 模板设置 `MICRONAUT_ENVIRONMENTS=development`
- **生产环境**：部署到 AWS 时，通过 CDK 设置 `MICRONAUT_ENVIRONMENTS=production` 