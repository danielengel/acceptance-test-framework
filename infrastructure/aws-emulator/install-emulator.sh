#!/bin/bash
# Build-time helper invoked by the Dockerfile, not on container startup.
# Inputs: TARGETARCH (Docker BuildKit), EMULATOR_VERSION, optional EMULATOR_PACKAGE_URL.
# Installs the RPM's aws-emulator executable; temporary downloads are removed on exit.
# Propagate command/pipeline failures and reject missing configuration.
set -euo pipefail

# Translate Docker architecture names into AWS package naming.
case "${TARGETARCH:?Docker target architecture is required}" in
    arm64) package_arch=aarch64 ;;
    amd64) package_arch=x86_64 ;;
    *) echo "Unsupported architecture: $TARGETARCH" >&2; exit 1 ;;
esac

# A mirror URL can override the standard version-pinned AWS download.
package_url="${EMULATOR_PACKAGE_URL:-https://downloads.aws.com/aws-emulator/packages/aws-emulator/LTS/aws-emulator-${EMULATOR_VERSION:?Emulator version is required}-1-${package_arch}.rpm}"
# Use a unique temporary path and remove it even if download/install fails.
package_file=$(mktemp --suffix=.rpm)
trap 'rm -f "$package_file"' EXIT

# Reject HTTP errors and follow download redirects before installing.
curl --fail --location "$package_url" --output "$package_file"
rpm --install "$package_file"
