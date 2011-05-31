#!/bin/bash

echo To master called >> /root/keepalived.log
/root/redundant_router/enable_pubip.sh
echo Enable public ip $? >> /root/keepalived.log
/root/redundant_router/primary-backup.sh primary
echo Switch conntrackd mode primary $? >> /root/keepalived.log
echo Status: MASTER >> /root/keepalived.log
