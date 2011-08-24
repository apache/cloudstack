#!/usr/bin/env bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 


sleep=60

while getopts s: OPTION
do
  case $OPTION in
  s)    sleep=$OPTARG
  esac
done

sleep $sleep