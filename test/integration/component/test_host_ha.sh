#!/bin/bash

#bring down all eth interfaces

usage() { echo "Usage: $0  <duration in seconds for downing all network interfaces>"; exit 1; }

case $1 in
    ''|*[!0-9]*) echo "The parameter should be an integer"; exit ;;
    *) echo $1 ;;
esac

if [ -z $1 ]; then
   usage
elif [ $1 -lt 1 ]; then
   echo "Down time should be at least 1 second"
   exit 1
elif [ $1 -gt 5000 ]; then
   echo "Down time should be less than 5000 second"
   exit 1
fi

for i in `ifconfig -a | sed 's/[ \t].*//;/^\(lo\|\)$/d' | grep eth`
do
   ifconfig $i down
done


service cloudstack-agent stop
update-rc.d -f cloudstack-agent remove

sleep $1

for i in `ifconfig -a | sed 's/[ \t].*//;/^\(lo\|\)$/d' | grep eth`
do
   ifconfig $i up
done


update-rc.d -f cloudstack-agent defaults
service cloudstack-agent start