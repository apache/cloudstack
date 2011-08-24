#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


host=""
account=""
templateid=""
storage=""
var=""

while getopts h:s:a:i: OPTION
do
  case $OPTION in
  a)    account="$OPTARG"
        ;;
  i)    templateid="$OPTARG"
                ;;
  h)    host="$OPTARG"
                ;;
  s)    storage="$OPTARG"
  esac
done

var=`ssh root@$host "cd $storage &&  mv template/tmpl/$account/$templateid/template.properties template/tmpl/$account/$templateid/template.properties1 && touch template/tmpl/$account/$templateid/template.properties"`


if  [ "$var" != "" ]
then echo "Was unable to corrupt template $path on host $host"
exit 2
else
exit 0
fi