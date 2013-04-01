#!/bin/bash

#
# Copyright 2010-2010 LinkedIn, Inc
# Portions Copyright (c) 2013 Yan Pujante
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

# locations
CURRDIR=`pwd`
BASEDIR=`cd $(dirname $0)/.. ; pwd`

cd $BASEDIR

LIB_DIR=lib

for file in `ls -1 $LIB_DIR/*.jar `; do
  if [ -z "$JVM_CLASSPATH" ]; then
    JVM_CLASSPATH="-classpath $JAVA_HOME/lib/tools.jar:$file"
  else
    JVM_CLASSPATH=$JVM_CLASSPATH:$file
  fi
done

OPTIONS=""
JVM_LOG4J=-Dlog4j.configuration=file:$BASEDIR/conf/log4j.xml
JVM_DEBUG=
#JVM_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

# set JAVA_HOME (JDK6) & JAVA_CMD
if [ -z "$JAVA_HOME" ]; then
  case $(uname -s) in
    SunOS )
      JAVA_HOME=/export/apps/jdk/JDK-1_6_0_16/usr/java
      JAVA_CMD=$JAVA_HOME/bin/$(isainfo -n)/java
      ;;
    Darwin)
      JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home
      JAVA_CMD=$JAVA_HOME/bin/java
      ;;
    * )
      if [ -z "$JAVA_HOME" ]
        then
          echo "Unknown platform (not SunOS or Darwin)!!! Please set JAVA_HOME manually."
          exit 1
        fi
      ;;
  esac
fi

if [ -z "$JAVA_CMD" ]; then
  JAVA_CMD=$JAVA_HOME/bin/java
fi

#echo $JAVA_CMD $JVM_LOG4J $JVM_DEBUG $JVM_CLASSPATH org.linkedin.zookeeper.cli.ClientMain $OPTIONS $@
$JAVA_CMD $JVM_LOG4J $JVM_DEBUG $JVM_CLASSPATH org.linkedin.zookeeper.cli.ClientMain $OPTIONS $@
