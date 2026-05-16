#!/usr/bin/env bash
# Build the arithmetic-gp project into an executable JAR.
# Tested on macOS Monterey with OpenJDK 21.

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Sanity check
if ! command -v javac >/dev/null 2>&1; then
    echo "Error: javac not found. Install a JDK (e.g. 'brew install openjdk@21')." >&2
    exit 1
fi

echo ">> Compiling sources..."
rm -rf out
mkdir -p out
javac -d out src/*.java

echo ">> Packaging arithmetic-gp.jar (Main-Class: TrainGP)..."
jar cfe arithmetic-gp.jar TrainGP -C out .

echo ">> Done. Run with:"
echo "     java -jar arithmetic-gp.jar           # training"
echo "     java -cp  arithmetic-gp.jar TestGP    # testing"
