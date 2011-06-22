#!/bin/bash

echo To fault called >> /root/keepalived.log
/root/redundant_router/disable_pubip.sh >> /root/keepalived.log 2>&1
/root/redundant_router/primary-backup.sh fault >> /root/keepalived.log 2>&1
echo Status: FAULT >> /root/keepalived.log
