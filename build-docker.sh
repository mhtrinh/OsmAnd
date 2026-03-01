#!/bin/bash

# Exit on error
set -e

IMAGE_NAME="osmand-build-env"

echo "Building Docker image: $IMAGE_NAME..."
docker build -t "$IMAGE_NAME" .

echo "Build complete."
