#!/bin/bash
# BananaCreat Build Script - Run on macOS/Linux with Android SDK installed

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_LOG="$PROJECT_DIR/build.log"

echo "================================================"
echo "BananaCreat Animation Studio - Build Script"
echo "================================================"
echo ""
echo "Project: $PROJECT_DIR"
echo "Time: $(date)"
echo ""

# Check prerequisites
echo "[1/5] Checking prerequisites..."
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found. Install JDK 17+."
    exit 1
fi

if ! command -v gradle &> /dev/null; then
    if [ ! -f "$PROJECT_DIR/gradlew" ]; then
        echo "ERROR: Gradle wrapper not found."
        exit 1
    fi
fi

echo "✓ Java: $(java -version 2>&1 | head -1)"
echo ""

# Clean build
echo "[2/5] Cleaning previous builds..."
cd "$PROJECT_DIR"
if [ -f "gradlew" ]; then
    ./gradlew clean --info >> "$BUILD_LOG" 2>&1 || true
else
    gradle clean --info >> "$BUILD_LOG" 2>&1 || true
fi
echo "✓ Clean complete"
echo ""

# Build debug APK
echo "[3/5] Building debug APK..."
if [ -f "gradlew" ]; then
    ./gradlew assembleDebug --info >> "$BUILD_LOG" 2>&1
else
    gradle assembleDebug --info >> "$BUILD_LOG" 2>&1
fi
echo "✓ Build complete"
echo ""

# Verify APK
echo "[4/5] Verifying APK..."
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "✓ APK found: $APK_PATH"
    echo "✓ Size: $APK_SIZE"
    echo ""
else
    echo "ERROR: APK not generated. Check build.log:"
    tail -50 "$BUILD_LOG"
    exit 1
fi

# Installation instructions
echo "[5/5] Installation instructions"
echo ""
echo "To install on a connected device or emulator:"
echo ""
echo "  adb install -r \"$APK_PATH\""
echo ""
echo "To run the app:"
echo ""
echo "  adb shell am start -n com.bananacreat/.MainActivity"
echo ""
echo "================================================"
echo "Build successful!"
echo "================================================"
echo ""
echo "APK ready at:"
echo "  $APK_PATH"
echo ""
