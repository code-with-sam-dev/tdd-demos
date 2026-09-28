#!/usr/bin/env bash
# Build the image and run the suite. The only prerequisite is Docker.
set -euo pipefail
cd "$(dirname "$0")"
docker build -t tdd-demos .
docker run --rm tdd-demos
