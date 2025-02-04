#!/bin/bash

# Build far jars.
./gradlew shadowJar

# Start local api gateway.
sam local start-api \
  --template local-env/templates/template.yaml \
  --port 3000 \
  --warm-containers LAZY
