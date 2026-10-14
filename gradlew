#!/bin/sh

APP_BASE_NAME=$(basename "$0")
APP_HOME=$(pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" \
    -Xmx64m -Xms64m \
    $JAVA_OPTS \
    $GRADLE_OPTS \
    -Dorg.gradle.appname=$APP_BASE_NAME \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
