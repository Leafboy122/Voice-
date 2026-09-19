#!/bin/sh
# Minimal gradle wrapper script
exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
