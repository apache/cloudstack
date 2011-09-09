#!/bin/bash

source /root/func.sh

lock="rrouter"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

echo To backup called >> /root/keepalived.log
/root/redundant_router/disable_pubip.sh >> /root/keepalived.log 2>&1
echo Disable public ip $? >> /root/keepalived.log
/root/redundant_router/primary-backup.sh backup >> /root/keepalived.log 2>&1
echo Switch conntrackd mode backup $? >> /root/keepalived.log
echo Status: BACKUP >> /root/keepalived.log

releaseLockFile $lock $locked
exit 0
