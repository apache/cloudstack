#!/bin/bash

source /root/func.sh

lock="rrouter"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

tail -n 1 /root/keepalived.log | grep "Status"

unlock_exit $? $lock $locked
