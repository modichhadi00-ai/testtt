#!/bin/sh
#
# Gradle wrapper script for Unix (used by GitHub Actions and Mac/Linux).

set -e
DIRNAME="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(cd "$DIRNAME" && pwd)"
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
