#!/bin/sh
# Disposable end-to-end run: start services, run Maven,
# then remove this Compose project's containers and volumes.
# Usage: ./scripts/acceptance.sh [additional Maven arguments]
# Requires Docker Compose and JDK 25+. Reports remain in acceptance-tests/target/.
# Stop on command errors or unset variables.
set -eu
# Resolve Compose files from the repository root, regardless of the caller's directory.
cd "$(dirname "$0")/.."
# Clean up on success, failure, or interruption; this also removes a manual stack
# using the same project name. Use init-services.sh separately to keep it running.
trap 'docker compose down -v' EXIT HUP INT TERM
# Prepare dependencies before running tests on the host JVM.
./scripts/init-services.sh
./scripts/prepare-host-tests.sh
# Forward extra arguments unchanged; preserve Maven's failure status.
./mvnw -pl acceptance-tests verify -Pacceptance "$@"
