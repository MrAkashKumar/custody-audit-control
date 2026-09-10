#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -x /usr/libexec/java_home ]; then JAVA_HOME=$(/usr/libexec/java_home -v 21); export JAVA_HOME; fi
mvn -q -DskipTests package
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" -jar cc-application/target/cc-application-1.0.0-SNAPSHOT.jar --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-dev,demo}" "$@"
