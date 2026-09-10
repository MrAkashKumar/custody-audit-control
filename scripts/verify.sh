#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -x /usr/libexec/java_home ]; then JAVA_HOME=$(/usr/libexec/java_home -v 21); export JAVA_HOME; fi
mvn verify
node --check cc-application/src/main/resources/static/app.js
