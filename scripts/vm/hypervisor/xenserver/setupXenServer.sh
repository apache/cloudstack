#!/bin/bash

# avoid disk full
mv /etc/cron.daily/logrotate /etc/cron.hourly 2>&1

# more aio thread
echo 1048576 >/proc/sys/fs/aio-max-nr

# empty heartbeat
cat /dev/null > /opt/xensource/bin/heartbeat

echo "success"

