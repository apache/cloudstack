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

if  [ $path != "" ] 
then
	var=`ssh root@$host "cd $storage && rm -rf $path"`
fi


if  [ "$var" != "" ]
then echo "Was unable to remove template $path from host $host"
exit 2
else
exit 0
fi