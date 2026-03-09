#!/bin/bash

set -e

# Configuration
CONTAINER_NAME="osmand-env"
IMAGE_NAME="osmand-build-env"
WORKSPACE="/workspace"
LOCAL_DIR="$(pwd)"

# Default settings
ABI="Arm64"
TYPE="Debug"

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --all) ABI="Fat" ;;
        --release) TYPE="Release" ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

VARIANT="androidFullLegacy${ABI}${TYPE}"

echo "============================================"
echo "Starting OsmAnd Build Orchestration"
echo "Target: $VARIANT"
echo "============================================"

# 1. Build Docker image if needed
echo "Step 1: Building/checking Docker image..."
chmod +x build-docker.sh
./build-docker.sh

# 2. Ensure Docker container is running
echo "Step 2: Ensuring Docker container '$CONTAINER_NAME' is running..."
chmod +x start-docker.sh
./start-docker.sh -n

# 3. Trigger the build inside the container
echo "Step 3: Triggering build inside the container..."
chmod +x build_final.sh
docker exec "$CONTAINER_NAME" /workspace/build_final.sh "$VARIANT"

# 4. Check if APK was generated
echo "Step 4: Locating generated APK on host..."
# Search specifically in the flavor and build type subdirectories
# For example: androidFullLegacyArm64/debug/
# VARIANT: androidFullLegacyArm64Debug -> flavor: androidFullLegacyArm64, buildType: debug
FLAVOR=$(echo "$VARIANT" | sed -E 's/(Debug|Release)$//')
BUILD_TYPE=$(echo "$TYPE" | tr '[:upper:]' '[:lower:]')
APK_PATH=$(find OsmAnd/build/outputs/apk -name "*.apk" | grep -i "$FLAVOR" | grep -i "$BUILD_TYPE" | head -n 1)

if [ -n "$APK_PATH" ]; then
    echo "============================================"
    echo "SUCCESS: Build finished!"
    echo "APK location on host: $APK_PATH"
    cp "$APK_PATH" "$LOCAL_DIR/"
    echo "APK copied to current directory: $LOCAL_DIR/$(basename "$APK_PATH")"
    echo "============================================"
else
    echo "============================================"
    echo "FAILURE: Could not find the generated APK for $VARIANT."
    echo "============================================"
    exit 1
fi
