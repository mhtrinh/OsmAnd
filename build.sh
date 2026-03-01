#!/bin/bash

set -e

# Configuration
CONTAINER_NAME="osmand-env"
IMAGE_NAME="osmand-build-env"
WORKSPACE="/workspace"
LOCAL_DIR="$(pwd)"

echo "============================================"
echo "Starting OsmAnd Build Orchestration"
echo "============================================"

# # 0. Clean up old APKs
# echo "Step 0: Cleaning up old APKs..."
# find OsmAnd/build/outputs/apk -name "*.apk" -delete 2>/dev/null || true
# rm -f OsmAnd-androidFull-legacy-fat-debug.apk

# 1. Build Docker image if needed
echo "Step 1: Building/checking Docker image..."
chmod +x build-docker.sh
./build-docker.sh

# 2. Ensure Docker container is running (using start-docker.sh)
echo "Step 2: Ensuring Docker container '$CONTAINER_NAME' is running..."
chmod +x start-docker.sh
./start-docker.sh -n

# 3. Trigger the build inside the container
echo "Step 3: Triggering build inside the container..."
# Using build_final.sh which is already inside the repository (mounted in /workspace)
chmod +x build_final.sh
docker exec "$CONTAINER_NAME" /workspace/build_final.sh

# 4. Check if APK was generated
echo "Step 4: Locating generated APK on host..."
APK_PATH=$(find OsmAnd/build/outputs/apk -name "*.apk" | head -n 1)

if [ -n "$APK_PATH" ]; then
    echo "============================================"
    echo "SUCCESS: Build finished!"
    echo "APK location on host: $APK_PATH"
    # Copy to local directory for convenience
    cp "$APK_PATH" "$LOCAL_DIR/"
    echo "APK copied to current directory: $LOCAL_DIR/$(basename "$APK_PATH")"
    echo "============================================"
else
    echo "============================================"
    echo "FAILURE: Could not find the generated APK on host."
    echo "============================================"
    exit 1
fi
