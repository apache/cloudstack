#!/bin/bash

echo To backup called >> /root/keepalived.log
/root/redundant_router/disable_pubip.sh
echo Disable public ip $? >> /root/keepalived.log
/root/redundant_router/primary-backup.sh backup
echo Switch conntrackd mode backup $? >> /root/keepalived.log
echo Status: BACKUP >> /root/keepalived.log
