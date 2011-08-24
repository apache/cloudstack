#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

cd /root/test/scripts && ./cleanparallel.sh && ./deploy.sh -i -d -r
if [ $? -ne 0 ]; then exit 2; fi;

echo 'sleeping for 5 min before executing regression test'
sleep 600
cd /root/test/scripts && ./regression.sh -type $1
