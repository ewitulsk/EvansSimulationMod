#!/bin/bash
set -e

echo "Building Phantom Engine..."
./gradlew build

JAR=$(find build/libs -name "*.jar" ! -name "*-sources.jar" | head -1)

if [ -z "$JAR" ]; then
    echo "Error: No jar found in build/libs"
    exit 1
fi

cp "$JAR" .
echo "Copied $(basename "$JAR") to project root"
