#!/bin/bash

 . /etc/cloudstack/management/tomcat6.conf
SERVLETENGINE='jetty'
# For SELinux we need to use 'runuser' not 'su'
if [ -x "/sbin/runuser" ]; then
    SU="/sbin/runuser -s /bin/sh"
else
    SU="/bin/su -s /bin/sh"
fi

touch $CLOUD_PID;
chown $CLOUD_USER. $CLOUD_PID
# CLASSPATH munging
if [ -n "$JSSE_HOME" ]; then
  CLASSPATH="${CLASSPATH}:$(build-classpath jcert jnet jsse 2>/dev/null)"
fi
CLASSPATH="${CLASSPATH}:$(build-classpath commons-daemon 2>/dev/null)"

CLOUD_HOME="/usr/share/cloudstack-management/"
CLOUD_BASE=$CLOUD_HOME

NAME="cloudstack-management"

# Define the tomcat username
CLOUD_USER="${CLOUD_USER:-cloud}"

# Define the tomcat group
CLOUD_GROUP="${CLOUD_GROUP:-`id -gn $CLOUD_USER`}"

# Define the tomcat log file
CLOUD_LOG="${CLOUD_LOG:-/var/log/${NAME}-initd.log}"

# Define the pid file name
# If change is needed, use sysconfig instead of here
export CLOUD_PID="${CLOUD_PID:-/var/run/${NAME}.pid}"
CLASSPATH="${CLASSPATH}:${CLOUD_HOME}/lib/jetty-runner.jar"
CLASS="org.eclipse.jetty.runner.Runner --classes /etc/cloudstack/management --path /client $CLOUD_HOME/webapps/client  --stop-port 8888 --stop-key monkeystop"

if [ "$1" = "start" ]; then
  java $JAVA_OPTS $CLOUD_OPTS \
    -classpath "$CLASSPATH" \
    -D"$SERVLETENGINE".base="$CLOUD_BASE" \
    -D"$SERVLETENGINE".home="$CLOUD_HOME" \
    -Djava.endorsed.dirs="$JAVA_ENDORSED_DIRS" \
    -Djava.io.tmpdir="$CLOUD_TMPDIR" \
    $CLASS \
    >> ${CLOUD_BASE}/logs/servlet-engine.out 2>&1 &
    if [ ! -z "$CLOUD_PID" ]; then
      echo $! > $CLOUD_PID
    fi
elif [ "$1" = "stop" ]; then
  echo Stopping server
  java -jar  ${CLOUD_HOME}/start.jar --stop -DSTOP.PORT=8888 -DSTOP.KEY=monkeystop
else
  echo "Usage: $0 {start|stop}"
  exit 1
fi
