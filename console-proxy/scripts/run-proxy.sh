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



 

#run.sh runs the console proxy.

# make sure we delete the old files from the original template 
rm console-proxy.jar
rm console-common.jar
rm conf/cloud.properties

CP=./:./conf
for file in *.jar
do
  CP=${CP}:$file
done

#CMDLINE=$(cat /proc/cmdline)
#for i in $CMDLINE
#  do
#     KEY=$(echo $i | cut -d= -f1)
#     VALUE=$(echo $i | cut -d= -f2)
#     case $KEY in
#       mgmt_host)
#          MGMT_HOST=$VALUE
#          ;;
#     esac
#  done
   
java -mx700m -cp $CP:./conf com.cloud.consoleproxy.ConsoleProxy $@
