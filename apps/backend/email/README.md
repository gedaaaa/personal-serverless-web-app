# Email Microservice

## Overview

The Email Microservice is responsible for handling all email sending operations within the Sunbath application. It provides a reliable, asynchronous way to send emails to users using the Resend.com API as the email delivery provider.

## Features

- Asynchronous email sending/scheduling via SQS queue
- Email sending history tracking
- Support for canceling of scheduled email
- Support for HTML email templates
- Configurable email settings

## Architecture

### Core Components

1. **EmailService Interface**: Defines the contract for sending emails

   - `ResendEmailService`: Production implementation using Resend.com API
   - `DoNothingEmailService`: Development implementation for local testing

2. **Email Record Persistence**: Stores a record of all sent emails

   - `EmailRecordRepository`: Interface for email record operations
   - Backed by DynamoDB for storage

3. **Lambda Handlers**:
   - `EmailFunctionHandler`: Processes SQS messages to send emails
   - `CancelEmailFunctionHandler`: Cancels scheduled emails

### Tech Stack

- **Language**: Kotlin
- **Framework**: Micronaut
- **Deployment**: AWS Lambda
- **Storage**: DynamoDB
- **Message Queue**: AWS SQS
- **Email Provider**: Resend.com API
- **Infrastructure as Code**: AWS CDK

## Configuration

### Environment Variables

- `MICRONAUT_ENVIRONMENTS`: Set to `dev` for local development, `production` for production deployment
- `JWT_SECRET`: Secret for JWT token generation/validation

### Configuration Files

- `application.yml`: Base configuration
- `application-dev.yml`: Development environment overrides
- `application-production.yml`: Production environment overrides

### Resend API Configuration

```yaml
resend:
  enabled: true # Set to true to enable email sending
  apiKey: ... # Set via parameter store in production
  fromEmail: 'noreply@sunbath.top'
  fromName: 'Sunbath Auth'
```

## API

### Email Data Structure

```kotlin
data class EmailData(
    val to: String,        // Recipient email address
    val from: String,      // Sender email address
    val subject: String,   // Email subject
    val html: String       // HTML content of the email
)
```

### SQS Message Format

```json
{
  "id": "unique-message-id",
  "data": {
    "to": "user@example.com",
    "from": "noreply@sunbath.top",
    "subject": "Email Subject",
    "html": "<html>Email content</html>"
  }
}
```

## Development

### Prerequisites

- JDK 21
- AWS CDK
- Local AWS credentials

### Running Locally

```bash
nx run email-service:dev
```

### Testing

```bash
nx run email-service:test
```

### Linting

```bash
nx run email-service:lint
```

### Formatting

```bash
nx run email-service:format
```

## Deployment

### Building the Application

```bash
nx run email-service:build
```

### Deploying to AWS

```bash
nx run email-service:deploy
```

### Infrastructure

The infrastructure is defined using AWS CDK in the `infra` directory. It provisions:

- Lambda functions
- SQS queues
- DynamoDB tables
- IAM roles and policies
- Parameter Store entries for secrets

## Error Handling

- Failed email attempts are stored with error information in DynamoDB
- SQS dead-letter queue handles messages that fail processing
- Resend API rate limiting (2 requests per second) is handled with appropriate delays

## Monitoring

- CloudWatch Logs for Lambda function logs
- CloudWatch Metrics for SQS queue metrics
- DynamoDB for email sending history
