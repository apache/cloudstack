#!/bin/bash

usage() {
  printf "Usage:\n %s <domR eth1 ip> \n" $(basename $0) >&2
  printf " %s <domR eth1 ip> \n" $(basename $0) >&2
}

cert="/root/.ssh/id_rsa.cloud"
domRIp=$1
shift

# check if gateway domain is up and running
check_gw() {
  ping -c 1 -n -q $1 > /dev/null
  if [ $? -gt 0 ]
  then
    sleep 1
    ping -c 1 -n -q $1 > /dev/null
  fi
  return $?;
}


# Check if DomR is up and running. If not, exit with error code 1.
check_gw "$domRIp"
if [ $? -gt 0 ]
then
  exit 1
fi

tmpfile=/tmp/$RANDOM.log

scp -P 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp:/root/keepalived.log $tmpfile
if [ $? -ne 0 ]
then
    exit $?
fi
result=`tail $tmpfile -n 1`
echo $result
