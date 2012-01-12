#!/bin/bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 




ret=0
# save previous state
  mv /etc/haproxy/haproxy.cfg /etc/haproxy/haproxy.cfg.old
  mv /var/run/haproxy.pid /var/run/haproxy.pid.old

  mv /etc/haproxy/haproxy.cfg.new /etc/haproxy/haproxy.cfg
  kill -TTOU $(cat /var/run/haproxy.pid.old)
  sleep 2
  if haproxy -D -p /var/run/haproxy.pid -f /etc/haproxy/haproxy.cfg; then
    logger -t cloud "New haproxy instance successfully loaded, stopping previous one."
    kill -KILL $(cat /var/run/haproxy.pid.old)
    rm -f /var/run/haproxy.pid.old
    ret=0
  else
    logger -t cloud "New instance failed to start, resuming previous one."
    kill -TTIN $(cat /var/run/haproxy.pid.old)
    rm -f /var/run/haproxy.pid
    mv /var/run/haproxy.pid.old /var/run/haproxy.pid
    mv /etc/haproxy/haproxy.cfg /etc/haproxy/haproxy.cfg.new
    mv /etc/haproxy/haproxy.cfg.old /etc/haproxy/haproxy.cfg
    ret=1
  fi

exit $ret

