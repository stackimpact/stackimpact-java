#!/bin/bash

set -e

rm -f /tmp/libstackimpact-*
$JAVA_HOME/bin/javac BasicApp.java

export SI_AGENT_KEY="agent key here"
export SI_APP_NAME="MyJavaApp"
$JAVA_HOME/bin/java -javaagent:/root/work/stackimpact-java/dist/stackimpact.jar -cp . BasicApp
