#!/bin/bash

echo To fault called >> /root/keepalived.log
/root/redundant_router/disable_pubip.sh 2>1&>> /root/keepalived.log && \
/root/redundant_router/primary-backup.sh fault 2>1&>> /root/keepalived.log
echo Status: FAULT >> /root/keepalived.log
