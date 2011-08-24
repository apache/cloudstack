#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

set -e
DST='src/'

java -cp ${DST}commons-httpclient-3.1.jar:${DST}mysql-connector-java-5.1.7-bin.jar:${DST}commons-logging-1.1.1.jar:${DST}commons-codec-1.4.jar:${DST}cloud-test.jar:${DST}log4j-1.2.15.jar:${DST}trilead-ssh2-build213.jar:${DST}cloud-utils.jar:./conf com.cloud.test.utils.SubmitCert -action DeleteCertificate $*
