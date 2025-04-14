#!/bin/bash

# Check if container already exists
if [ "$(docker ps -q -f name=dynamodb)" ]; then
    echo "DynamoDB container is already running."
    exit 0
elif [ "$(docker ps -aq -f status=exited -f name=dynamodb)" ]; then
    # Remove stopped container
    docker rm dynamodb
fi

docker run --rm -d --name dynamodb -p 8000:8000 amazon/dynamodb-local -jar DynamoDBLocal.jar -sharedDb -dbPath /home/dynamodblocal
