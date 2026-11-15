#!/bin/sh
# Export only credentials and public trust material for AWS SDK test calls.
set -eu
cd "$(dirname "$0")/.."
ready=false
for attempt in $(seq 1 60); do
    if [ "$(docker inspect --format '{{.State.Status}}:{{.State.ExitCode}}' "$(docker compose ps -a -q aws-init)" 2>/dev/null)" = exited:0 ] && docker compose exec -T aws-gateway curl -fsS --cacert /tls/ca.pem https://localhost:9443/__admin/health >/dev/null 2>&1; then
        ready=true
        break
    fi
    sleep 1
done
if [ "$ready" != true ]; then
    echo 'AWS initialization did not complete. Inspect docker compose logs aws-init aws-gateway aws-emulator.' >&2
    exit 1
fi
runtime_dir=acceptance-tests/target/host-runtime
umask 077
mkdir -p "$runtime_dir"
chmod 700 "$runtime_dir"
docker compose cp aws-emulator:/home/emulator/.aws/emulator/9297/emulator_credentials "$runtime_dir/emulator_credentials"
docker compose cp aws-gateway:/tls/truststore.p12 "$runtime_dir/truststore.p12"
docker compose cp aws-gateway:/tls/ca.pem "$runtime_dir/ca.pem"
chmod 600 "$runtime_dir"/*
printf 'Host test files ready in %s\n' "$runtime_dir"
