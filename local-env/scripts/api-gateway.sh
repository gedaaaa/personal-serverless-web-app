#!/bin/bash

# Stop existing api gateway.
lsof -i:3000 | awk 'NR!=1 {print $2}' | xargs kill
# Start local api gateway.
sam local start-api \
  --template local-env/templates/template.yaml \
  --port 3000 \
  --warm-containers LAZY 
