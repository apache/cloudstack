#!/usr/bin/env bash
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
