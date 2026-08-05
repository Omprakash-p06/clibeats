#!/usr/bin/env bash
set -euo pipefail

echo "=========================================="
echo "   CLIBeats Quality Gate Verification    "
echo "=========================================="

echo "[1/4] Checking code formatting (ktlint)..."
./gradlew ktlintCheck

echo "[2/4] Running Detekt static analysis..."
./gradlew detekt

echo "[3/4] Running Android Lint..."
./gradlew lintDebug

echo "[4/4] Running Unit Tests..."
./gradlew testDebugUnitTest

echo "=========================================="
echo " SUCCESS: All Quality Gates Passed!      "
echo "=========================================="
