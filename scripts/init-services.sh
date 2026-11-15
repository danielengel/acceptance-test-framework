#!/bin/sh
# Build and start the Compose services; resource initialization runs in the one-shot aws-init service.
# Usage: ./scripts/init-services.sh
# Leaves services running. Does not export host test files or execute tests.
# Fail immediately on command errors or unset variables.
set -eu
# Compose paths are relative to the repository root.
cd "$(dirname "$0")/.."
# Build changed images and start in the background. Compose dependencies gate
# application startup on successful AWS resource initialization.
docker compose up -d --build
