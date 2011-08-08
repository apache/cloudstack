#!/bin/bash

LOCK=/tmp/rrouter.lock
locked=0

# Wait the lock
for i in `seq 1 5`
do
    if [ ! -e $LOCK ]
    then
        touch $LOCK
        locked=1
        break
    fi
    sleep 1
    echo sleep 1
done

if [ $locked -eq 0 ]
then
    echo Status: fail to get the lock! >> /root/keepalived.log
    exit
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
    rm $LOCK
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

rm $LOCK
