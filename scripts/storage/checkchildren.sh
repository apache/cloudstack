#!/usr/bin/env bash
# $Id: checkchildren.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/checkchildren.sh $
# checkchdilren.sh -- Does this path has children?
# 

usage() {
  printf "Usage:  %s path \n" $(basename $0) >&2
}

if [ $# -ne 1 ]
then
  usage
  exit 1
fi

#set -x

fs=$1
if [ "${fs:0:1}" != "/" ]
then
  fs="/"$fs
fi

if [ -d $fs ]
then
  if [ `ls -l $fs | grep -v total | wc -l | awk '{print $1}'` -eq 0 ]
  then
    exit 0
  else
    exit 1
  fi
fi

exit 0
