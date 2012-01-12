#!/usr/bin/env bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 



 

# run the usage server

PATHSEP=':'
if [[ $OSTYPE == "cygwin" ]]; then
  PATHSEP=';'
  export CATALINA_HOME=`cygpath -m $CATALINA_HOME`
fi

CP=./$PATHSEP$CATALINA_HOME/webapps/client/WEB-INF/lib/vmops-server.jar
CP=${CP}$PATHSEP$CATALINA_HOME/webapps/client/WEB-INF/lib/vmops-server-extras.jar
CP=${CP}$PATHSEP$CATALINA_HOME/webapps/client/WEB-INF/lib/vmops-utils.jar
CP=${CP}$PATHSEP$CATALINA_HOME/webapps/client/WEB-INF/lib/vmops-core.jar
CP=${CP}$PATHSEP$CATALINA_HOME/webapps/client/WEB-INF/lib/vmops-usage.jar
CP=${CP}$PATHSEP$CATALINA_HOME/conf

for file in $CATALINA_HOME/lib/*.jar; do
  CP=${CP}$PATHSEP$file
done

#echo CP is $CP
DEBUG_OPTS=
#DEBUG_OPTS=-Xrunjdwp:transport=dt_socket,address=$1,server=y,suspend=n

java -cp $CP $DEBUG_OPTS -Dcatalina.home=${CATALINA_HOME} -Dpid=$$ com.vmops.usage.UsageServer $*
