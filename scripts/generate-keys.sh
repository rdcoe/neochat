#!/bin/bash
# Generate RSA key pair for JWT signing

set -e

KEYS_DIR=${1:-./keys}

echo "Generating RSA key pair in $KEYS_DIR"

# Create keys directory if it doesn't exist
mkdir -p "$KEYS_DIR"

# Generate private key (2048 bits)
openssl genrsa -out "$KEYS_DIR/private.pem" 2048

# Extract public key from private key
openssl rsa -in "$KEYS_DIR/private.pem" -outform PEM -pubout -out "$KEYS_DIR/public.pem"

# Set appropriate permissions
chmod 600 "$KEYS_DIR/private.pem"
chmod 644 "$KEYS_DIR/public.pem"

echo "Keys generated successfully:"
echo "  Private key: $KEYS_DIR/private.pem"
echo "  Public key:  $KEYS_DIR/public.pem"

# Display key fingerprint for verification
echo ""
echo "Public key fingerprint:"
openssl rsa -pubin -in "$KEYS_DIR/public.pem" -text -noout | grep "Public-Key"
