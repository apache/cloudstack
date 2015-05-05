#!/bin/bash

 . /etc/sysconfig/cloudstack-management

# For SELinux we need to use 'runuser' not 'su'
if [ -x "/sbin/runuser" ]; then
    SU="/sbin/runuser -s /bin/sh"
else
    SU="/bin/su -s /bin/sh"
fi

if [ -r /usr/share/java-utils/java-functions ]; then
  . /usr/share/java-utils/java-functions
else
  echo "Can't read Java functions library, aborting"
  exit 1
fi
touch $CATALINA_PID;
chown $TOMCAT_USER. $CATALINA_PID
set_javacmd

# CLASSPATH munging
if [ -n "$JSSE_HOME" ]; then
  CLASSPATH="${CLASSPATH}:$(build-classpath jcert jnet jsse 2>/dev/null)"
fi
CLASSPATH="${CLASSPATH}:${CATALINA_HOME}/bin/bootstrap.jar"
CLASSPATH="${CLASSPATH}:${CATALINA_HOME}/bin/tomcat-juli.jar"
CLASSPATH="${CLASSPATH}:$(build-classpath commons-daemon 2>/dev/null)"

if [ "$1" = "start" ]; then
  ${JAVACMD} $JAVA_OPTS $CATALINA_OPTS \
    -classpath "$CLASSPATH" \
    -Dcatalina.base="$CATALINA_BASE" \
    -Dcatalina.home="$CATALINA_HOME" \
    -Djava.endorsed.dirs="$JAVA_ENDORSED_DIRS" \
    -Djava.io.tmpdir="$CATALINA_TMPDIR" \
    -Djava.util.logging.config.file="${CATALINA_BASE}/conf/logging.properties" \
    -Djava.util.logging.manager="org.apache.juli.ClassLoaderLogManager" \
    org.apache.catalina.startup.Bootstrap start \
    >> ${CATALINA_BASE}/logs/catalina.out 2>&1 &
    if [ ! -z "$CATALINA_PID" ]; then
      echo $! > $CATALINA_PID
    fi
elif [ "$1" = "stop" ]; then
  ${JAVACMD} $JAVA_OPTS $CATALINA_OPTS \
    -classpath "$CLASSPATH" \
    -Dcatalina.base="$CATALINA_BASE" \
    -Dcatalina.home="$CATALINA_HOME" \
    -Djava.endorsed.dirs="$JAVA_ENDORSED_DIRS" \
    -Djava.io.tmpdir="$CATALINA_TMPDIR" \
    org.apache.catalina.startup.Bootstrap stop \
    >> ${CATALINA_BASE}/logs/catalina.out 2>&1 &
else
  echo "Usage: $0 {start|start-security|stop|version}"
  exit 1
fi
