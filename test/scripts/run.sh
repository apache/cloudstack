#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

PATHSEP=':'
if [[ $OSTYPE == "cygwin" ]] ; then
  PATHSEP=';'
fi

#directory with jar files
DST='../src/'

CP=${DST}commons-httpclient-3.1.jar${PATHSEP}${DST}commons-logging-1.1.1.jar${PATHSEP}${DST}commons-codec-1.4.jar${PATHSEP}${DST}cloud-test.jar${PATHSEP}${DST}log4j.jar${PATHSEP}${DST}trilead-ssh2-build213.jar${PATHSEP}${DST}cloud-utils.jar${PATHSEP}.././conf
java -cp $CP com.cloud.test.stress.TestClientWithAPI $*
#java -cp $CP com.cloud.test.stress.StressTestDirectAttach $*

