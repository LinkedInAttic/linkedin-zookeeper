#!/bin/bash
#
# Customized LinkedIn ZooKeeper ENV
#

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This script should be sourced into other zookeeper
# scripts to setup the env variables

ZOOCFGDIR="$ZOOBINDIR/../conf"

ZOOCFG="zoo.cfg"

ZOOCFG="$ZOOCFGDIR/$ZOOCFG"

ZOO_LOG_DIR="$ZOOBINDIR/../logs"

ZOO_LOG4J_PROP="INFO,CONSOLE"

ZOOLIBDIR=${ZOOLIBDIR:-$ZOOBINDIR/../lib}
for i in "$ZOOLIBDIR"/*.jar
do
    CLASSPATH="$CLASSPATH:$i"
done

# Add conf dir to the classpath (to find zoo.cfg)
CLASSPATH=$ZOOCFGDIR:$CLASSPATH

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

