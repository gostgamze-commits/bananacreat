#!/bin/bash
set -e

echo "=== BananaCreat APK Build ==="
echo "Step 1: Cleaning..."
./gradlew clean 2>&1 | tail -5

echo "Step 2: Building debug APK..."
./gradlew assembleDebug 2>&1 | tail -20

echo "Step 3: Locating APK..."
find build -name "*debug*.apk" -type f 2>/dev/null | head -1

echo "=== Build Complete ==="
