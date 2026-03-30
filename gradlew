#!/bin/sh
GRADLE_HOME="/home/ubuntu/.gradle/wrapper/dists/gradle-8.9"
exec "$GRADLE_HOME/bin/gradle" "$@"
