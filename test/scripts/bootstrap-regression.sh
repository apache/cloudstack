#!/bin/bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


set -e
set -x

# at the moment, my PWD is /root

mypwd=`dirname $0`
cd "$mypwd"/regression  # now my PWD is /root/artifacts/regression or wherever I was copied

unzip test.zip
tar zxvmf ../cloud-management-dist.tar.gz
cd scripts
./cleanparallel.sh
./deploy.sh  -b ../cloud-management-dist -d -r
sleep 300
./regression.sh