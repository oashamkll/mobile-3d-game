#!/usr/bin/env bash
set -euo pipefail
if command -v ./gradlew >/dev/null 2>&1 && [[ -x ./gradlew ]]; then
  ./gradlew :android:assembleDebug
else
  gradle :android:assembleDebug
fi
