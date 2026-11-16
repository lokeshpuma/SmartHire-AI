#!/usr/bin/env bash
set -e

if [ -z "$JAVA_HOME" ]; then
    if [ -x "/usr/libexec/java_home" ]; then
        JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home)
    fi
fi

echo "Running full test and AI evaluation suite..."
export JAVA_HOME
mvn test
