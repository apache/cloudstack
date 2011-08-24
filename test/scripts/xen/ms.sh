#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


host=""
operation=""

while getopts h:o: OPTION
do
  case $OPTION in
  h)	host="$OPTARG"
  ;;
  o)	operation="$OPTARG"
  esac
done

var=`ssh root@$host "service cloud-management $operation"`
sleep 30

exit 0
fi