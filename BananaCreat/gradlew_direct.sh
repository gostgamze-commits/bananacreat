#!/bin/bash
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
cd "$(dirname "$0")"
"$JAVA_HOME/bin/java" -Xmx2048m -Xms1024m -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
