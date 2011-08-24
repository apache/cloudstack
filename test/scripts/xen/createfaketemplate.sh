#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


host=""
account=""
templateid=""
storage=""
var=""
dir=""

while getopts h:s:a:i:d: OPTION
do
  case $OPTION in
  a)    account="$OPTARG"
        ;;
  i)    templateid="$OPTARG"
                ;;
  h)    host="$OPTARG"
        ;;
  s)    storage="$OPTARG"
  		;;
  d)	dir="$OPTARG"
  esac
done

var=`ssh root@$host "cd $storage &&  cp -rf template/tmpl/$account/$templateid template/tmpl/$account/$dir`

if  [ "$var" != "" ]
then echo "Was unable to create fake template on host $host"
exit 2
else
exit 0
fi