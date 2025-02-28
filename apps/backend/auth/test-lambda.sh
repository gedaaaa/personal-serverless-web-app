#!/bin/bash

# Build the application
./gradlew :apps:backend:auth:app:optimizedJitJarAll

# Set variables
LAMBDA_JAR="apps/backend/auth/app/build/libs/app-0.1-all-optimized.jar"
HANDLER="io.micronaut.function.aws.MicronautRequestStreamHandler"
EVENT_FILE="apps/backend/auth/test-event.json"

# Create a test event if it doesn't exist
if [ ! -f "$EVENT_FILE" ]; then
  cat > "$EVENT_FILE" << EOF
{
  "resource": "/health",
  "path": "/health",
  "httpMethod": "GET",
  "headers": {
    "Accept": "*/*",
    "Content-Type": "application/json"
  },
  "requestContext": {
    "identity": {
      "sourceIp": "127.0.0.1"
    }
  }
}
EOF
fi

# Invoke the Lambda function locally
aws lambda invoke \
  --cli-binary-format raw-in-base64-out \
  --function-name auth \
  --payload file://$EVENT_FILE \
  --no-sign-request \
  --endpoint http://localhost:9000 \
  response.json

# Display the response
cat response.json
rm response.json 