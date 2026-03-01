#!/bin/bash

CONTAINER_NAME="osmand-env"
IMAGE_NAME="osmand-build-env"
WORKSPACE="/workspace"
REPO_DIR="$(pwd)"
RESOURCES_DIR="$(cd .. && pwd)/OsmAnd-resources"
HIST_FILE="${REPO_DIR}/.bash_history_docker"

FORCE_RESTART=false
NO_EXEC=false

while getopts "fn" opt; do
  case $opt in
    f)
      FORCE_RESTART=true
      ;;
    n)
      NO_EXEC=true
      ;;
    *)
      echo "Usage: $0 [-f] [-n] [command...]"
      exit 1
      ;;
  esac
done
shift $((OPTIND-1))

# Ensure history file exists
touch "$HIST_FILE"

# Check if image exists
if [[ "$(docker images -q $IMAGE_NAME 2> /dev/null)" == "" ]]; then
  echo "Image $IMAGE_NAME not found. Please run ./build-docker.sh first."
  exit 1
fi

# Force restart logic
if [ "$FORCE_RESTART" = true ]; then
    if [ "$(docker ps -aq -f name=^/${CONTAINER_NAME}$)" ]; then
        echo "Forcing restart: removing existing container..."
        docker rm -f "$CONTAINER_NAME"
    fi
fi

# Check if container exists
if [ "$(docker ps -aq -f name=^/${CONTAINER_NAME}$)" ]; then
    # Container exists, check if it's running
    if [ "$(docker ps -q -f name=^/${CONTAINER_NAME}$)" ]; then
        echo "Container is already running."
    else
        echo "Starting existing container..."
        docker start "$CONTAINER_NAME"
    fi
else
    # Container does not exist, create and run it
    echo "Creating and starting new container..."
    docker run -d \
        --name "$CONTAINER_NAME" \
        -v "$REPO_DIR:$WORKSPACE" \
        -v "$RESOURCES_DIR:/resources" \
        -v "$HIST_FILE:/home/osmand/.bash_history" \
        -v ~/.gemini:/home/osmand/.gemini \
        -w "$WORKSPACE" \
        "$IMAGE_NAME" \
        tail -f /dev/null
fi

if [ "$NO_EXEC" = true ]; then
    exit 0
fi

# Enter the container or run command
if [ $# -eq 0 ]; then
    echo "Entering container: $CONTAINER_NAME..."
    docker exec -it "$CONTAINER_NAME" bash
else
    echo "Running command in container: $CONTAINER_NAME..."
    docker exec "$CONTAINER_NAME" "$@"
fi

