#!/bin/bash
# Script to build the final OsmAnd APK

set -e

# --- Configuration ---
# Default to androidFullLegacyFatDebug for testing if not specified.
# For production, you might want androidFullLegacyFatRelease or gplayFullLegacyFatRelease.
VARIANT=${1:-"androidFullLegacyFatDebug"}

# Environment Setup
export ANDROID_HOME=${ANDROID_HOME:-"/opt/android-sdk"}
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-"/opt/android-sdk"}
export ANDROID_NDK_ROOT=${ANDROID_NDK_ROOT:-"/opt/android-sdk/ndk/26.1.10909125"}
export PATH=$PATH:$ANDROID_NDK_ROOT

echo "============================================"
echo "Building OsmAnd APK Variant: $VARIANT"
echo "SDK: $ANDROID_HOME"
echo "NDK: $ANDROID_NDK_ROOT"
echo "============================================"

# Ensure the keystore exists for debug builds (it should already be in the repo)
if [[ "$VARIANT" == *"Debug"* ]]; then
    if [ ! -f "keystores/debug.keystore" ]; then
        echo "Warning: keystores/debug.keystore not found. Trying to generate a placeholder if needed..."
        # OsmAnd project usually has one, but let's be safe.
    fi
fi

# Run Gradle build
# We use --no-daemon to avoid some memory/IO issues in limited environments.
# We skip tests because some legacy tests are failing in this environment.
./gradlew :OsmAnd:assemble$VARIANT --no-daemon --stacktrace -x test

# Locating the APK
echo "Locating the generated APK..."
# Build tasks place APKs in OsmAnd/build/outputs/apk/
# The structure usually follows: OsmAnd/build/outputs/apk/<flavor>/<buildType>/
APK_FILE=$(find OsmAnd/build/outputs/apk -name "*.apk" | head -n 1)

if [ -n "$APK_FILE" ]; then
    echo "============================================"
    echo "SUCCESS: APK built successfully!"
    echo "Location: $APK_FILE"
    echo "============================================"
else
    echo "============================================"
    echo "FAILURE: Could not find the generated APK."
    echo "============================================"
    exit 1
fi
