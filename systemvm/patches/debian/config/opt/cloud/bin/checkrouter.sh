#!/usr/bin/env bash

STATUS=$(cat /etc/cloudstack/cmdline.json | grep redundant_state | awk '{print $2;}' | sed -e 's/[,\"]//g')
if [ "$?" -ne "0" ]
then
	   STATUS=MASTER
fi
echo "Status: ${STATUS}&Bumped: NO"
