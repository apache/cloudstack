#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


uuid=""
name=""
host=""
var=""

while getopts u:n:h: OPTION
do
  case $OPTION in
  u)    uuid="$OPTARG"
        ;;
  n)    name="$OPTARG"
  		;;
  h)	host="$OPTARG"
  esac
done



if [ "$uuid" != "" ]
then
        var=`ssh root@$host "xe vm-list uuid=$uuid"`
else
        if [ "$name" != "" ]
        then
                var=`ssh root@$host "xe vm-list name-label=$name"`
        fi
fi


if  [ "$var" == "" ]
then echo "VM $name doesn't exist on host $host"
exit 2
else
exit 0
fi