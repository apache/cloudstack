#!/bin/bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


while true
do
sleep 600
pid=`ps -ef | grep 'management.jmxremote' | grep -v 'cloud-management' | grep -v grep | awk '{print \$2}'`
if grep -q java.lang.OutOfMemoryError /var/log/vmops/vmops.log
then
while true
do
t=$(date  +"%h%d_%H_%M_%S")
jmap -dump:format=b,file=/root/dump/heap.bin.$t $pid
sleep 1800
done
fi
done