#!/bin/bash

set -e

rm -f /tmp/libstackimpact-*
$JAVA_HOME/bin/javac -cp ../../dist/stackimpact.jar DemoApp.java
$JAVA_HOME/bin/java -cp ../../dist/stackimpact.jar:. DemoApp
