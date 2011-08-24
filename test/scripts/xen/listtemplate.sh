#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


host=""
path=""
storage=""
var=""

while getopts h:s:p: OPTION
do
  case $OPTION in
  p)    path="$OPTARG"
        ;;
  h)	host="$OPTARG"
  		;;
  s)	storage="$OPTARG"
  esac
done

	var=`ssh root@$host "ls -ltr $storage/$path"`

if  [ "$var"  == "" ]
then echo "Template $path doesn't exist on storage host $host"
exit 2
else
exit 0
fi