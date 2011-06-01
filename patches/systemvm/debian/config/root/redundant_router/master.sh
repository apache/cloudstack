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
/root/redundant_router/enable_pubip.sh
echo Enable public ip $? >> /root/keepalived.log
/root/redundant_router/primary-backup.sh primary
echo Switch conntrackd mode primary $? >> /root/keepalived.log
echo Status: MASTER >> /root/keepalived.log

rm $LOCK
