#!/bin/bash
set -euo pipefail

# Reuse certificates across restarts. Keep private keys in /private-tls and
# share only public trust files in /tls. Requires keytool and openssl.
# Run in a subshell so the early exit and restrictive umask apply only to TLS setup.
configure_tls() (
    # These paths and the development password match NGINX and Java client settings.
    alias_name=aws-gateway
    password=changeit
    server_store=/private-tls/server.p12
    private_key=/private-tls/server.key
    certificate=/tls/ca.pem
    trust_store=/tls/truststore.p12

    # A gateway restart must keep the certificate already trusted by running clients.
    if [ -s "$private_key" ] && [ -s "$certificate" ] && [ -s "$trust_store" ]; then
        exit 0
    fi
    umask 077
    mkdir -p /private-tls
    chmod 700 /private-tls
    # Replace previous stores rather than reusing a stale key or certificate alias.
    rm -f "$server_store" "$trust_store"

    # Create a self-signed certificate valid for Docker DNS and host loopback access.
    keytool -genkeypair \
        -alias "$alias_name" -keyalg RSA -keysize 2048 -validity 365 \
        -dname CN=aws-gateway \
        -ext SAN=dns:aws-gateway,dns:localhost,ip:127.0.0.1 \
        -storetype PKCS12 -keystore "$server_store" -storepass "$password" -noprompt

    # PEM format is read by NGINX, curl, and the AWS CLI.
    keytool -exportcert -rfc \
        -alias "$alias_name" -keystore "$server_store" -storepass "$password" \
        -file "$certificate"

    # NGINX needs a separate unencrypted private-key file inside this container.
    openssl pkcs12 \
        -in "$server_store" -passin "pass:$password" \
        -nocerts -nodes -out "$private_key"

    # Build a certificate-only Java trust store; it contains no private key.
    keytool -importcert \
        -alias "$alias_name" -file "$certificate" -keystore "$trust_store" \
        -storetype PKCS12 -storepass "$password" -noprompt

    # Public trust material must be readable by application users.
    chmod 644 "$certificate" "$trust_store"
    rm -f "$server_store"
)

configure_tls
exec nginx -c /bootstrap/nginx.conf -g 'daemon off;'
