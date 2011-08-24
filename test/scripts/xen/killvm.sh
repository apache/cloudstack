#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


uuid=""
name=""
host=""

while getopts n:h: OPTION
do
  case $OPTION in
  n)    name="$OPTARG"
  		;;
  h)	host="$OPTARG"
  esac
done



if [ "$name" != "" ]
then
        uuid=`ssh root@$host "xe vm-list name-label=$name | grep uuid | awk '{print \\$5}'"`
fi

echo "uuid is $uuid"
var=`ssh root@$host "xe vm-shutdown uuid=$uuid; xe vm-destroy uuid=$uuid"`

if  [ "$var" != "" ]
then echo "Was unable to destroy the vm with name $name and uuid $uuid on host $host"
exit 2
else
exit 0
fi