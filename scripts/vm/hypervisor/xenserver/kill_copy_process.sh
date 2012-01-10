#!/bin/bash
# Version @VERSION@

#set -x
 
usage() {
  printf "Usage: %s [name label]  \n" $(basename $0) 
}

if [ -z $1 ]; then
  usage
  echo "3#no namelabel"
  exit 0
else
  namelabel=$1
fi

pid=`ps -ef | grep "dd" | grep $namelabel | grep -v "grep" | awk '{print $2}'`
if [ -z $pid ]; then
  echo "true"
  exit 0
fi 

kill -9 $pid
if [ $? -ne 0 ]; then
  echo "false"
  exit 0
fi
echo "true"
exit 0
