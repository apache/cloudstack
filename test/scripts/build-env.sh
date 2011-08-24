#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

PATHSEP=':'
if [[ $OSTYPE == "cygwin" ]] ; then
  PATHSEP=';'
fi

CP=commons-httpclient-3.1.jar${PATHSEP}commons-logging-1.1.1.jar${PATHSEP}commons-codec-1.3.jar${PATHSEP}testclient.jar${PATHSEP}log4j-1.2.15.jar${PATHSEP}utils.jar${PATHSEP}./conf
java -cp $CP com.vmops.test.longrun.BuildGuestNetwork $*


