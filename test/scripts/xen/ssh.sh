#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


PATHSEP=':'
if [[ $OSTYPE == "cygwin" ]] ; then
  PATHSEP=';'
fi

DST='../src/'
password=""
host=""
url=""

while getopts p:h:u: OPTION
do
  case $OPTION in
  p)    password="$OPTARG"
  		;;
  h)	host="$OPTARG"
  		;;
  u)    url="$OPTARG"
  esac
done


CP=${DST}commons-httpclient-3.1.jar${PATHSEP}${DST}commons-logging-1.1.1.jar${PATHSEP}${DST}commons-codec-1.4.jar${PATHSEP}${DST}cloud-test.jar${PATHSEP}${DST}log4j-1.2.15.jar${PATHSEP}${DST}trilead-ssh2-build213.jar${PATHSEP}${DST}cloud-utils.jar${PATHSEP}.././conf
java -cp $CP com.cloud.test.stress.SshTest $*
