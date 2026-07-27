#!/usr/bin/env bash
#
# Generates a self-signed, dev-only PKCS12 keystore for the gateway's TLS termination.
# The keystore is gitignored (spring-cloud/gateway/.gitignore) since private keys should
# never be committed, even self-signed local ones. Re-run this whenever you clone the repo
# or need a fresh key.
#
set -e

KEYSTORE_DIR="spring-cloud/gateway/src/main/resources/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/edge.p12"
STOREPASS="OrderManagementSystem"
ALIAS="localhost"

mkdir -p "$KEYSTORE_DIR"

keytool -genkeypair \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore "$KEYSTORE_FILE" \
  -storepass "$STOREPASS" \
  -validity 3650 \
  -dname "CN=localhost, OU=OrderManagementSystem, O=OrderManagementSystem, L=NoviSad, ST=Vojvodina, C=RS"

echo "Keystore generated at $KEYSTORE_FILE"
