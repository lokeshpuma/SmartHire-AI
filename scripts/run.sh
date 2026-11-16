#!/usr/bin/env bash
set -e

# Detect Java 21+ home
if [ -z "$JAVA_HOME" ]; then
    if [ -x "/usr/libexec/java_home" ]; then
        JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home)
    fi
fi

echo "Starting Intelligent Resume Screening Engine with JAVA_HOME=${JAVA_HOME}..."
export JAVA_HOME
mvn spring-boot:run
