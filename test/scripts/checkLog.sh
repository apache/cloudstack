#!/bin/bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


#set -x

rm -rf log.out

dir=$(dirname "$0")
if [ -f $dir/../deploy.properties ]; then
  . "$dir/../deploy.properties"
fi


echo "Starting checking the logs for errors...."

  for i in $SERVER
  do
    echo -ne "Log from Management server $i: \n=====================================================================================\n" >> log.out
    ssh root@$i "grep -i -E 'error|exce|fail|unable|leak' /var/log/vmops/vmops.log" >> log.out
    echo -e "\n" >> log.out
  done

  for i in $COMPUTE
  do
    echo -ne "Log from Computing host $i: \n=====================================================================================\n" >> log.out
    ssh root@$i "grep -i -E 'error|exce|fail|timed out|unable' /var/log/vmops/agent.log" >> log.out
    echo -e "\n\n" >> log.out
  done

  for i in $ROUTER
  do
    echo -ne "Log from Routing host $i: \n=====================================================================================\n" >> log.out
    ssh root@$i "grep -i -E 'error|exce|fail|timed out|unable' /var/log/vmops/agent.log" >> log.out
    echo -e "\n" >> log.out
  done

  for i in $STORAGE
  do
    echo -ne "Log from Storage host $i: \n=====================================================================================\n" >> log.out
    ssh root@$i "grep -i error /var/log/vmops/agent.log" >> log.out
    ssh root@$i "grep -i fail /var/log/vmops/agent.log" >> log.out
    ssh root@$i "grep -i exce /var/log/vmops/agent.log" >> log.out
    ssh root@$i "grep -i unable /var/log/vmops/agent.log" >> log.out
	ssh root@$i "grep -i \'timed out\' /var/log/vmops/agent.log" >> log.out
    echo -e "\n" >> log.out
  done


echo "Done! Check log.out file for the results"
