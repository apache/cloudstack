#!/bin/bash

 . /etc/cloudstack/management/tomcat.conf
SERVLETENGINE=$(cat /etc/cloudstack/management/servlet-engine)
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

# Servlet specific startup config
if [ "$SERVLETENGINE" == "jetty" ] ; then
    CLASSPATH="${CLASSPATH}:${CLOUD_HOME}/lib/jetty-runner.jar"
    CLASS="org.eclipse.jetty.runner.Runner --classes /etc/cloudstack/management --path /client $CLOUD_HOME/webapps/client" 
else
    CLASSPATH="${CLASSPATH}:${CLOUD_HOME}/bin/bootstrap.jar"
    CLASSPATH="${CLASSPATH}:${CLOUD_HOME}/bin/tomcat-juli.jar"
    CLASS="org.apache.catalina.startup.Bootstrap start"
    CLOUD_OPTS="${CLOUD_OPTS} -Djava.util.logging.config.file=${CLOUD_BASE}/conf/logging.properties"
    CLOUD_OPTS="${CLOUD_OPTS} -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"
    CLOUD_OPTS="${CLOUD_OPTS} -Djava.security.properties=${CLOUD_BASE}/conf/java.security.ciphers"
fi

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
  java $JAVA_OPTS $CLOUD_OPTS \
    -classpath "$CLASSPATH" \
    -Dcatalina.base="$CLOUD_BASE" \
    -Dcatalina.home="$CLOUD_HOME" \
    -Djava.endorsed.dirs="$JAVA_ENDORSED_DIRS" \
    -Djava.io.tmpdir="$CLOUD_TMPDIR" \
    org.apache.catalina.startup.Bootstrap stop \
    >> ${CLOUD_BASE}/logs/servlet-engine.out 2>&1 &
else
  echo "Usage: $0 {start|stop}"
  exit 1
fi
