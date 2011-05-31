#!/bin/bash

echo To fault called >> /root/keepalived.log
/root/redundant_router/disable_pubip.sh && \
/root/redundant_router/primary-backup.sh fault
echo Status: FAULT >> /root/keepalived.log
