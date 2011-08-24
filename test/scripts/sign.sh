#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

set -e
DST='../src/'

java -cp ${DST}cloud-test.jar:${DST}cloud-utils.jar com.cloud.test.utils.SignRequest $*
