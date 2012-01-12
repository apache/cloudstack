#!/bin/bash
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
