#!/bin/bash

source /root/func.sh

lock="rrouter"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

echo To master called >> /root/keepalived.log
/root/redundant_router/enable_pubip.sh >> /root/keepalived.log 2>&1
ret=$?
last_msg=`tail -n 1 /root/keepalived.log`
echo Enable public ip returned $ret >> /root/keepalived.log
if [ $ret -ne 0 ]
then
    echo Fail to enable public ip! >> /root/keepalived.log
    ifconfig eth2 down
    service keepalived stop >> /root/keepalived.log 2>&1
    service conntrackd stop >> /root/keepalived.log 2>&1
    echo Status: FAULT \($last_msg\) >> /root/keepalived.log
    releaseLockFile $lock $locked
    exit
fi
/root/redundant_router/primary-backup.sh primary >> /root/keepalived.log 2>&1
ret=$?
echo Switch conntrackd mode primary returned $ret >> /root/keepalived.log
if [ $ret -ne 0 ]
then
    echo Fail to switch conntrackd mode, but try to continue working >> /root/keepalived.log
fi
echo Status: MASTER >> /root/keepalived.log

releaseLockFile $lock $locked
exit 0
